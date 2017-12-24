package com.sai.tictactoe

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private val TAG: String = MainActivity::class.java.simpleName

    var player1 = ArrayList<Int>()
    var player2 = ArrayList<Int>()
    var activePlayer = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
    }
}
