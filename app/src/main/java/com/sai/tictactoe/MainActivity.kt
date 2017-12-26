package com.sai.tictactoe

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    private val TAG: String = MainActivity::class.java.simpleName

    private val SP_NAME = "com.sai.tictactoe"
    private val SP_KEY = "game_play"

    private val handler = Handler()

    private val player1 = ArrayList<Int>()
    private val player2 = ArrayList<Int>()
    private var activePlayer = 1

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    private var mDatabase = FirebaseDatabase.getInstance()
    private val dbRef = mDatabase.reference

    private lateinit var currentUserEmail: String
    private lateinit var currentUserId: String

    private lateinit var sessionId: String
    private lateinit var playerSymbol: String

    private var currentGamePlay: GamePlay = GamePlay.AI

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        currentUserEmail = intent.extras.getString(ARG_EMAIL)
        currentUserId = intent.extras.getString(ARG_ID)

        incomingRequests()

        sharedPreferences = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

        if(sharedPreferences.contains(SP_KEY)) {
            currentGamePlay = GamePlay.valueOf(sharedPreferences.getString(SP_KEY, GamePlay.AI.name))
        }
    }

    fun buttonClick(v: View) {
        val buttonClicked = v as Button
        val cellId = when(buttonClicked.id) {
            R.id.button1 -> 1
            R.id.button2 -> 2
            R.id.button3 -> 3
            R.id.button4 -> 4
            R.id.button5 -> 5
            R.id.button6 -> 6
            R.id.button7 -> 7
            R.id.button8 -> 8
            R.id.button9 -> 9
            else -> 0
        }

        Log.d(TAG, "Clicked button $cellId")
        // Commenting out for now, we can have both online and local playing based on settings.
        if(currentGamePlay == GamePlay.AI) {
            playGame(cellId, buttonClicked)
        } else {
            dbRef.child(ONLINE_PLAY).child(sessionId).child(KEY + cellId.toString()).setValue(currentUserEmail)
        }
    }

    fun acceptButtonClick(v: View) {
        var requestedUserEmail = email_edit_text.getString()
        dbRef.child(USERS).child(splitEmail(requestedUserEmail)).child(REQUEST).push().setValue(currentUserEmail)

        playOnline(splitEmail(requestedUserEmail) + splitEmail(currentUserEmail)) // will create the same node to play
        playerSymbol = "O"
    }

    fun requestButtonClick(v: View) {
        if(email_edit_text.isValidEmail()) {
            var email = email_edit_text.getString()
            dbRef.child(USERS).child(splitEmail(email)).child(REQUEST).push().setValue(currentUserEmail)

            playOnline(splitEmail(currentUserEmail) + splitEmail(email))
            playerSymbol = "X"

        } else {
            email_edit_text.error = "Please enter a valid email"
        }
    }

    fun playGame(cellId: Int, selectedButton: Button) {
        selectedButton.isEnabled = false

        if(activePlayer == 1) {
            selectedButton.text = "X"
            player1.add(cellId)
            activePlayer = 2
        } else {
            selectedButton.text = "O"
            player2.add(cellId)
            activePlayer = 1
        }

        if(checkWinner()) {
            Log.d(TAG, "Game over")
            resetGame()
            return
        }

        if(currentGamePlay == GamePlay.AI && activePlayer == 2) autoPlay()
    }

    fun checkWinner() : Boolean{
        var winner = -1

        // row 1
        if(player1.contains(1) && player1.contains(2) && player1.contains(3)) {
            winner = 1
        }

        if(player2.contains(1) && player2.contains(2) && player2.contains(3)) {
            winner = 2
        }

        //row 2
        if(player1.contains(4) && player1.contains(5) && player1.contains(6)) {
            winner = 1
        }

        if(player2.contains(4) && player2.contains(5) && player2.contains(6)) {
            winner = 2
        }

        //row 3
        if(player1.contains(7) && player1.contains(8) && player1.contains(9)) {
            winner = 1
        }

        if(player2.contains(7) && player2.contains(8) && player2.contains(9)) {
            winner = 2
        }

        //Col 1
        if(player1.contains(1) && player1.contains(4) && player1.contains(7)) {
            winner = 1
        }

        if(player2.contains(1) && player2.contains(4) && player2.contains(7)) {
            winner = 2
        }

        //Col 2
        if(player1.contains(2) && player1.contains(5) && player1.contains(8)) {
            winner = 1
        }

        if(player2.contains(2) && player2.contains(5) && player2.contains(8)) {
            winner = 2
        }

        //Col 3
        if(player1.contains(3) && player1.contains(6) && player1.contains(9)) {
            winner = 1
        }

        if(player2.contains(3) && player2.contains(6) && player2.contains(9)) {
            winner = 2
        }

        //Diagonal 1
        if(player1.contains(1) && player1.contains(5) && player1.contains(9)) {
            winner = 1
        }

        if(player2.contains(1) && player2.contains(5) && player2.contains(9)) {
            winner = 2
        }

        //Diagonal 2
        if(player1.contains(3) && player1.contains(5) && player1.contains(7)) {
            winner = 1
        }

        if(player2.contains(3) && player2.contains(5) && player2.contains(7)) {
            winner = 2
        }

        if(winner != -1) {
            if(winner == 1) {
                showGameOverMessage("Player 1 wins the game")
            } else {
                showGameOverMessage("Player 2 wins the game")
            }
            return true
        }
        return false
    }

    fun autoPlay() {
        handler.postDelayed({
            val emptyCells = ArrayList<Int>()
            (1..9).filterTo(emptyCells) { !player1.contains(it) && !player2.contains(it) }

            val r = Random()
            val randIndex = r.nextInt(emptyCells.size - 0) + 0

            val cellId = emptyCells[randIndex]

            val buttonToSelect= when(cellId) {
                1 -> button1
                2 -> button2
                3 -> button3
                4 -> button4
                5 -> button5
                6 -> button6
                7 -> button7
                8 -> button8
                9 -> button9
                else -> null
            }
            playGame(cellId, buttonToSelect!!)
        }, 1000)
    }

    fun playForOpponent(cellId: Int) {
        val buttonToSelect= when(cellId) {
            1 -> button1
            2 -> button2
            3 -> button3
            4 -> button4
            5 -> button5
            6 -> button6
            7 -> button7
            8 -> button8
            9 -> button9
            else -> null
        }
        playGame(cellId, buttonToSelect!!)
    }

    private fun resetGame() {
        player1.clear()
        player2.clear()

        if(currentGamePlay == GamePlay.ONLINE) {
            dbRef.child(ONLINE_PLAY).child(sessionId).setValue(null)
        }

        // Clear board
        clearAllButtons()
    }

    private fun clearAllButtons() {
        handler.postDelayed({
            button1.reset()
            button2.reset()
            button3.reset()
            button4.reset()
            button5.reset()
            button6.reset()
            button7.reset()
            button8.reset()
            button9.reset()
        }, 3000)
    }

    fun incomingRequests() {
        dbRef.child(USERS).child(splitEmail(currentUserEmail)).child(REQUEST)
                .addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError?) {

                    }

                    override fun onDataChange(data: DataSnapshot?) {
                        try {
                            if (data != null) {
                                val values = data.value as HashMap<String, Any>
                                var requestEmail: String = values[values.keys.last()] as String
                                email_edit_text.setText(requestEmail)

                                dbRef.child(USERS).child(splitEmail(currentUserEmail)).child(REQUEST).setValue(true)
                            }
                        } catch (ex: Exception) {

                        }
                    }

                })
    }

    fun playOnline(sessionId: String) {
        this.sessionId = sessionId

        dbRef.child(ONLINE_PLAY).child(sessionId)
                .addValueEventListener(object: ValueEventListener {
                    override fun onCancelled(p0: DatabaseError?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onDataChange(data: DataSnapshot?) {
                        try {
                            if (data != null) {
                                val values = data.value as HashMap<String, Any>

                                var player = values[values.keys.last()]

                                activePlayer = if(player != currentUserEmail) {
                                    if(playerSymbol === "X") 1 else 2
                                } else {
                                    if(playerSymbol === "X") 2 else 1
                                }

                                playForOpponent(values.keys.last().split("-")[1].toInt())
                            }
                        } catch (ex: Exception) {
                            Log.d(TAG, ex.printStackTrace().toString())
                        }
                    }
                })
    }

    fun showMessage(msg:String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_play_ai -> {
                sharedPreferences.edit().putString(SP_KEY, GamePlay.AI.name).apply()
            }

            R.id.menu_play_online -> {
                sharedPreferences.edit().putString(SP_KEY, GamePlay.ONLINE.name).apply()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if(currentGamePlay === GamePlay.AI) {
            menu!!.findItem(R.id.menu_play_online).isVisible = true
            menu!!.findItem(R.id.menu_play_ai).isVisible = false
        } else {
            menu!!.findItem(R.id.menu_play_online).isVisible = false
            menu!!.findItem(R.id.menu_play_ai).isVisible = true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    fun showGameOverMessage(msg: String) {
        val snackbar = Snackbar.make(main_layout, msg, Snackbar.LENGTH_LONG)
        snackbar.show()
    }
}

fun Button.reset() {
    this.text = ""
    this.isEnabled = true
}