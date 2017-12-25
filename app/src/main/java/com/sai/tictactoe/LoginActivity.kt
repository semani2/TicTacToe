package com.sai.tictactoe

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private val TAG = LoginActivity::class.java.simpleName

    private lateinit var mAuth: FirebaseAuth

    private var mDatabase = FirebaseDatabase.getInstance()
    private val dbRef = mDatabase.reference

    private var mCurrentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
    }

    override fun onStart() {
        super.onStart()
        mCurrentUser = mAuth.currentUser
        goToMain()
    }

    fun loginClicked(v: View) {
        if(email_edit_text.isBlank() || !email_edit_text.isValidEmail()) {
            email_edit_text.error = "Please enter a valid email"
            return
        }

        if(password_edit_text.isBlank() || !password_edit_text.isValidPassword()) {
            password_edit_text.error = "Please enter a valid password"
            return
        }

        loginWithFirebase(email_edit_text.getString(), password_edit_text.getString())
    }

    fun loginWithFirebase(email: String, password: String) {
        mAuth.let {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            goToMain()
                        } else {
                            showMessage("There was an error logging in. Please verify your email and password.")
                        }
                    }
        }
    }

    fun goToMain() {
        val currentUser: FirebaseUser? = mAuth.currentUser

        if(currentUser != null) {

            //Save user in Firebase database
            dbRef.child(users).child(currentUser.uid).setValue(currentUser.email)

            val mainIntent = Intent(this, MainActivity::class.java)
            mainIntent.putExtra(ARG_EMAIL, currentUser.email)
            mainIntent.putExtra(ARG_ID, currentUser.uid)

            startActivity(mainIntent)

            showMessage("Welcome!")
        }
    }
}

private fun EditText.getString(): String = this.text.toString()
private fun EditText.isBlank() = this.text.toString().isBlank()
private fun EditText.isValidEmail() = !this.isBlank()
        && Patterns.EMAIL_ADDRESS.matcher(this.text.toString()).matches()
private fun EditText.isValidPassword() = !this.isBlank() && this.text.toString().length >= 6

fun Activity.showMessage(msg:String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
