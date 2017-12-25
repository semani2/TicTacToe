package com.sai.tictactoe

import android.app.Activity
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG: String = MainActivity::class.java.simpleName

    private val handler = Handler()

    private val player1 = ArrayList<Int>()
    private val player2 = ArrayList<Int>()
    private var activePlayer = 1

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
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
        playGame(cellId, buttonClicked)
    }

    fun playGame(cellId: Int, selectedButton: Button) {
        selectedButton.isEnabled = false

        if(activePlayer == 1) {
            selectedButton.text = "X"
            selectedButton.setBackgroundColor(Color.GREEN)
            player1.add(cellId)
            activePlayer = 2
        } else {
            selectedButton.text = "O"
            selectedButton.setBackgroundColor(Color.BLUE)
            player2.add(cellId)
            activePlayer = 1
        }

        if(checkWinner()) {
            Log.d(TAG, "Game over")
            resetBoard()
            return
        }

        if(activePlayer == 2) autoPlay()
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
                showMessage("Player 1 wins the game")
            } else {
                showMessage("Player 2 wins the game")
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

    private fun resetBoard() {

    }

    fun showMessage(msg:String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
