package com.sai.tictactoe

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import android.widget.EditText
import android.support.v7.widget.ShareActionProvider;
import android.text.Html
import android.widget.TextView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {

    private val TAG: String = MainActivity::class.java.simpleName

    private val SP_NAME = "com.sai.tictactoe"
    private val SP_KEY = "game_play"

    private val handler = Handler()

    private val player1 = ArrayList<Int>()
    private val player2 = ArrayList<Int>()
    private var activePlayer = 1

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    private lateinit var mAuth: FirebaseAuth

    private var mDatabase = FirebaseDatabase.getInstance()
    private val dbRef = mDatabase.reference

    private lateinit var currentUserEmail: String
    private lateinit var currentUserId: String

    private var sessionId: String? = null
    private var playerSymbol: String? = null

    private var currentGamePlay: GamePlay = GamePlay.AI

    private lateinit var sharedPreferences: SharedPreferences

    private var shouldShowTurnText: Boolean = false

    private var multiplayerGameOn: Boolean = false

    private val keysList: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        mAuth = FirebaseAuth.getInstance()

        currentUserEmail = intent.extras.getString(ARG_EMAIL)
        currentUserId = intent.extras.getString(ARG_ID)

        incomingRequests()

        sharedPreferences = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

        if(sharedPreferences.contains(SP_KEY)) {
            currentGamePlay = GamePlay.valueOf(sharedPreferences.getString(SP_KEY, GamePlay.AI.name))
        }

        if(currentGamePlay == GamePlay.ONLINE) {
            request_fab.visibility = View.VISIBLE
        }

        showTurnTextIfNecessary()

        MobileAds.initialize(this, "ca-app-pub-8188678914265559~1551055943")
        val adRequest = AdRequest.Builder().build()
        ad_view.loadAd(adRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        resetGame()
    }

    private fun showTurnTextIfNecessary() {
        shouldShowTurnText = isCurrentPlayerTurn()

        turn_text_view.visibility = if(shouldShowTurnText) View.VISIBLE else View.GONE
    }

    private fun isCurrentPlayerTurn(): Boolean {
        return if(currentGamePlay == GamePlay.AI) {
            activePlayer == 1
        } else {
            ((activePlayer == 1 && playerSymbol != null && playerSymbol == "O")
                    || (activePlayer == 2 && playerSymbol != null && playerSymbol == "X"))
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
            if(!isCurrentPlayerTurn()) return
            playGame(cellId, buttonClicked)
        } else {
            if(sessionId != null) {
                if(!isCurrentPlayerTurn()) return
                dbRef.child(ONLINE_PLAY).child(sessionId).child(KEY + cellId.toString()).setValue(currentUserEmail)
            } else {
                showMessage("Send a request to start a game")
            }
        }
    }

    private fun acceptButtonClick(editText: EditText) {
        var requestedUserEmail = editText.getString()
        //dbRef.child(USERS).child(splitEmail(requestedUserEmail)).child(REQUEST).push().setValue(currentUserEmail)

        playOnline(splitEmail(requestedUserEmail) + splitEmail(currentUserEmail)) // will create the same node to play
        playerSymbol = "O"
        toggleCurrentGamePlay(GamePlay.ONLINE)
        invalidateOptionsMenu()
        showTurnTextIfNecessary()
        showPlayerName(requestedUserEmail)
        request_fab.visibility = View.GONE
    }

    private fun requestButtonClick(editText: EditText) {
        if(editText.isValidEmail()) {
            var email = editText.getString()
            dbRef.child(USERS).child(splitEmail(email)).child(REQUEST).push().setValue(currentUserEmail)

            playOnline(splitEmail(currentUserEmail) + splitEmail(email))
            playerSymbol = "X"
            showTurnTextIfNecessary()
            showPlayerName(email)
            request_fab.visibility = View.GONE
        } else {
            editText.error = "Please enter a valid email"
        }
    }

    private fun showPlayerName(email: String) {
        game_text_view.text = getString(R.string.str_game_on, email)
        game_text_view.visibility = View.VISIBLE
    }

    private fun playGame(cellId: Int, selectedButton: Button) {
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
        showTurnTextIfNecessary()

        if(checkWinner()) {
            Log.d(TAG, "Game over")
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
        } else {
            val emptyCells = ArrayList<Int>()
            (1..9).filterTo(emptyCells) { !player1.contains(it) && !player2.contains(it) }

            if(emptyCells.size == 0) {
                showGameOverMessage("Well played! Game draw")
                return true
            }
            return false
        }
    }

    fun autoPlay() {
        handler.postDelayed({
            val emptyCells = ArrayList<Int>()
            (1..9).filterTo(emptyCells) { !player1.contains(it) && !player2.contains(it) }

            /*val r = Random()
            val randIndex = r.nextInt(emptyCells.size - 0) + 0*/

            //val cellId = emptyCells[randIndex]
            val cellId = getCellId(emptyCells)

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

    private fun getCellId(emptyCells: ArrayList<Int>): Int {
        var cellId: Int = -1
        var winnerId:Int = -1

        // row 1
        if((player1.contains(1) && player1.contains(2)) && emptyCells.contains(3)) {
            cellId = 3
        }

        if((player2.contains(1) && player2.contains(2)) && emptyCells.contains(3)) {
            cellId = 3
            winnerId = 3
        }

        if((player1.contains(1) && player1.contains(3)) && emptyCells.contains(2)) {
            cellId = 2
        }

        if( (player2.contains(1) && player2.contains(3)) && emptyCells.contains(2)) {
            winnerId = 2
            cellId = 2
        }

        if(player1.contains(2) && player1.contains(3) &&  emptyCells.contains(1)) {
            cellId = 1
        }

        if(player2.contains(2) && player2.contains(3) &&  emptyCells.contains(1)) {
            cellId = 1
            winnerId = 1
        }

        // row 2
        if((player1.contains(4) && player1.contains(5)) && emptyCells.contains(6)) {
            cellId = 6
        }
        if((player2.contains(4) && player2.contains(5))  && emptyCells.contains(6)) {
            winnerId = 6
            cellId = 6
        }

        if((player1.contains(4) && player1.contains(6)) && emptyCells.contains(5)) {
            cellId = 5
        }

        if((player2.contains(4) && player2.contains(6))  && emptyCells.contains(5)) {
            winnerId = 5
            cellId = 5
        }

        if(player1.contains(5) && player1.contains(6) && emptyCells.contains(4)) {
            cellId = 4
        }

        if(player2.contains(5) && player2.contains(6) && emptyCells.contains(4)) {
            winnerId = 4
            cellId = 4
        }

        // row 3
        if((player1.contains(7) && player1.contains(8)) && emptyCells.contains(9)) {
            cellId = 9
        }
        if((player2.contains(7) && player2.contains(9)) && emptyCells.contains(9)) {
            winnerId = 9
            cellId = 9
        }

        if((player1.contains(7) && player1.contains(9)) && emptyCells.contains(8)) {
            cellId = 8
        }
        if((player2.contains(7) && player2.contains(9)) && emptyCells.contains(8)) {
            winnerId = 8
            cellId = 8
        }

        if(player1.contains(8) && player1.contains(9) && emptyCells.contains(7)) {
            cellId = 7
        }
        if(player2.contains(8) && player2.contains(9) && emptyCells.contains(7)) {
            winnerId = 7
            cellId = 7
        }


        // Col 1
        if((player1.contains(1) && player1.contains(4))  && emptyCells.contains(7)) {
            cellId = 7
        }
        if((player2.contains(1) && player2.contains(4)) && emptyCells.contains(7)) {
            winnerId = 7
            cellId = 7
        }

        if((player1.contains(1) && player1.contains(7)) && emptyCells.contains(4)) {
            cellId = 4
        }
        if((player2.contains(1) && player2.contains(7)) && emptyCells.contains(4)) {
            winnerId = 4
            cellId = 4
        }

        if(player1.contains(4) && player1.contains(7) && emptyCells.contains(1)) {
            cellId = 1
        }

        if(player2.contains(4) && player2.contains(7) && emptyCells.contains(1)) {
            winnerId = 1
            cellId = 1
        }

        // Col 2
        if((player1.contains(2) && player1.contains(5)) && emptyCells.contains(8)) {
            cellId = 8
        }
        if( (player2.contains(2) && player2.contains(5)) && emptyCells.contains(8)) {
            winnerId = 8
            cellId = 8
        }

        if((player1.contains(2) && player1.contains(8)) && emptyCells.contains(5)) {
            cellId = 5
        }
        if((player2.contains(2) && player2.contains(8)) && emptyCells.contains(5)) {
            winnerId = 5
            cellId = 5
        }

        if(player1.contains(5) && player1.contains(8) && emptyCells.contains(2)) {
            if( player2.contains(5) && player2.contains(8) && emptyCells.contains(2)) {
                winnerId = 2
            }
            cellId = 2
        }

        // Col 2
        if((player1.contains(3) && player1.contains(6)) && emptyCells.contains(9)) {
            cellId = 9
        }
        if( (player2.contains(3) && player2.contains(6)) && emptyCells.contains(9)) {
            winnerId = 9
            cellId = 9
        }

        if((player1.contains(3) && player1.contains(9)) && emptyCells.contains(6)) {
            cellId = 6
        }
        if((player2.contains(3) && player2.contains(9)) && emptyCells.contains(6)) {
            winnerId = 6
            cellId = 6
        }

        if(player1.contains(6) && player1.contains(9) && emptyCells.contains(3)) {
            cellId = 3
        }
        if( player2.contains(6) && player2.contains(9) && emptyCells.contains(3)) {
            winnerId = 3
            cellId = 3
        }

        // Diagonal 1
        if((player1.contains(1) && player1.contains(5)) && emptyCells.contains(9)) {
            cellId = 9
        }
        if((player2.contains(1) && player2.contains(5)) && emptyCells.contains(9)) {
            winnerId = 9
            cellId = 9
        }

        if((player1.contains(5) && player1.contains(9)) && emptyCells.contains(1)) {
            cellId = 1
        }
        if( (player2.contains(5) && player2.contains(9)) && emptyCells.contains(1)) {
            winnerId = 1
            cellId = 1
        }

        if(player1.contains(1) && player1.contains(9) && emptyCells.contains(5)) {
            cellId = 5
        }
        if(player2.contains(1) && player2.contains(9) && emptyCells.contains(5)) {
            winnerId = 5
            cellId = 5
        }

        // Diagonal 1
        if((player1.contains(3) && player1.contains(5)) && emptyCells.contains(7)) {
            cellId = 7
        }
        if((player2.contains(3) && player2.contains(5)) && emptyCells.contains(7)) {
            winnerId = 7
            cellId = 7
        }

        if((player1.contains(5) && player1.contains(7)) && emptyCells.contains(3)) {
            cellId = 3
        }
        if( (player2.contains(5) && player2.contains(7)) && emptyCells.contains(3)) {
            winnerId = 3
            cellId = 3
        }

        if(player1.contains(3) && player1.contains(7) && emptyCells.contains(5)) {
            cellId = 5
        }
        if(player2.contains(3) && player2.contains(7) && emptyCells.contains(5)) {
            winnerId = 5
            cellId = 5
        }

        if(winnerId != -1 && emptyCells.contains(winnerId)) {
            return winnerId
        }

        if(cellId == -1 || !emptyCells.contains(cellId)) {
            return emptyCells[0]
        }

        return cellId
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
        keysList.clear()

        if(currentGamePlay == GamePlay.ONLINE && sessionId != null) {
            dbRef.child(ONLINE_PLAY).child(sessionId).setValue(null)
            sessionId = null
            playerSymbol = null
            request_fab.visibility = View.VISIBLE
        } else {
            activePlayer = 1
        }

        clearAllButtons()
        showTurnTextIfNecessary()
        game_text_view.visibility = View.GONE
    }

    private fun clearAllButtons() {
        button1.reset()
        button2.reset()
        button3.reset()
        button4.reset()
        button5.reset()
        button6.reset()
        button7.reset()
        button8.reset()
        button9.reset()
    }

    fun incomingRequests() {
        dbRef.child(USERS).child(splitEmail(currentUserEmail)).child(REQUEST)
                .addValueEventListener(object: ValueEventListener {
                    override fun onCancelled(p0: DatabaseError?) {

                    }

                    override fun onDataChange(data: DataSnapshot?) {
                        try {
                            if (data != null) {
                                val values = data.value as HashMap<String, Any>
                                var requestEmail: String = values[values.keys.elementAt(values.keys.size - 1)] as String
                                showAcceptDialog(requestEmail)

                                dbRef.child(USERS).child(splitEmail(currentUserEmail)).child(REQUEST).setValue(true)
                            }
                        } catch (ex: Exception) {

                        }
                    }

                })
    }

    private fun showLicensesDialog() {
        val inflater = layoutInflater
        val dialoglayout = inflater.inflate(R.layout.licenses_dialog_layout, null)

        val licenseTextView = dialoglayout.findViewById<TextView>(R.id.licenses_text_view)
        licenseTextView.text = Html.fromHtml("<div>Icons made by <a href=\"http://www.freepik.com\" title=\"Freepik\">Freepik</a> from <a href=\"https://www.flaticon.com/\" title=\"Flaticon\">www.flaticon.com</a> is licensed by <a href=\"http://creativecommons.org/licenses/by/3.0/\" title=\"Creative Commons BY 3.0\" target=\"_blank\">CC 3.0 BY</a></div>")

        val okButton = dialoglayout.findViewById<Button>(R.id.ok_button)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialoglayout)

        val dialog = builder.create()
        okButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun showAcceptDialog(requestEmail: String) {
        val inflater = layoutInflater
        val dialoglayout = inflater.inflate(R.layout.request_dialog_layout, null)

        val requestAcceptButton = dialoglayout.findViewById<Button>(R.id.request_accept_button)
        val playerEmailEditText = dialoglayout.findViewById<EditText>(R.id.player_email_edit_text)
        val requestTextView = dialoglayout.findViewById<TextView>(R.id.request_text_view)

        requestTextView.visibility = View.VISIBLE

        requestAcceptButton.text = getString(R.string.str_accept)

        playerEmailEditText.setText(requestEmail)
        playerEmailEditText.isEnabled = false

        val builder = AlertDialog.Builder(this)
        builder.setView(dialoglayout)

        val dialog = builder.create()
        requestAcceptButton.setOnClickListener {
            acceptButtonClick(playerEmailEditText)
            dialog.dismiss()
        }

        dialog.show()
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

                                for(entry in values) {
                                    var key = entry.key

                                    if(!keysList.contains(key)) {
                                        keysList.add(key)
                                        var player = entry.value
                                        activePlayer = if(player != currentUserEmail) {
                                            if(playerSymbol === "X") 1 else 2
                                        } else {
                                            if(playerSymbol === "X") 2 else 1
                                        }

                                        playForOpponent(entry.key.split("-")[1].toInt())
                                        break
                                    }
                                }
                            }
                        } catch (ex: Exception) {
                            // Error on server reset game with message
                            //showMessage("Your opponent has quit the game. Please start a new game.")
                            //handler.postDelayed({resetGame()}, 3000)
                            Log.d(TAG, ex.printStackTrace().toString())
                        }
                    }
                })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Locate MenuItem with ShareActionProvider
        val item = menu.findItem(R.id.menu_share)

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Join me in playing one of the coolest games. Download X's and O's from the play store.")
        sendIntent.type = "text/plain"

        val mShareActionProvider = MenuItemCompat.getActionProvider(item) as ShareActionProvider
        mShareActionProvider.setShareIntent(sendIntent)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_play_ai -> {
                toggleCurrentGamePlay(GamePlay.AI)
                resetGame()
                showTurnTextIfNecessary()
            }

            R.id.menu_play_online -> {
                toggleCurrentGamePlay(GamePlay.ONLINE)
                resetGame()
                showTurnTextIfNecessary()
            }

            R.id.menu_logout -> {
                mAuth.signOut()
                goToLogin()
            }

            R.id.menu_send_feedback -> {
                var mailString: String ="mailto:chithalabs@gmail.com" +
                        "?subject=" + Uri.encode(SUBJECT)

                val emailIntent = Intent(Intent.ACTION_SENDTO)
                emailIntent.data = Uri.parse(mailString)

                try {
                    startActivity(emailIntent)
                }
                catch(e: Exception) {
                    showMessage("Sorry! Could not find application to share feedback.")
                }
            }

            R.id.menu_licenses -> {
                showLicensesDialog()
            }
        }
        invalidateOptionsMenu()
        return super.onOptionsItemSelected(item)
    }

    private fun goToLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java)
        loginIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

        startActivity(loginIntent)
        finish()
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

    fun showMessage(msg: String) {
        val snackbar = Snackbar.make(main_layout, msg, Snackbar.LENGTH_LONG)
        snackbar.show()
    }

    fun showGameOverMessage(msg: String) {
        var snackbar: Snackbar
        if(currentGamePlay == GamePlay.AI) {
            snackbar = Snackbar.make(main_layout, msg, Snackbar.LENGTH_INDEFINITE)
            snackbar.setAction("Play again", {
                resetGame()
            })
        } else {
            snackbar = Snackbar.make(main_layout, msg, Snackbar.LENGTH_LONG)
            handler.postDelayed({
                resetGame()
            }, 3000)
        }
        snackbar.show()
    }

    fun requestFABClick(v: View) {
        val inflater = layoutInflater
        val dialoglayout = inflater.inflate(R.layout.request_dialog_layout, null)

        val requestAcceptButton = dialoglayout.findViewById<Button>(R.id.request_accept_button)
        val playerEmailEditText = dialoglayout.findViewById<EditText>(R.id.player_email_edit_text)

        requestAcceptButton.text = getString(R.string.str_request)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialoglayout)

        val dialog = builder.create()
        requestAcceptButton.setOnClickListener {
            requestButtonClick(playerEmailEditText)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun toggleCurrentGamePlay(gameplay: GamePlay) {
        currentGamePlay = gameplay
        sharedPreferences.edit().putString(SP_KEY, gameplay.name).apply()
        request_fab.visibility = if(gameplay == GamePlay.AI) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}

fun Button.reset() {
    this.text = ""
    this.isEnabled = true
}