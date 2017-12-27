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

        progress_bar.visibility = View.VISIBLE
        loginWithFirebase(email_edit_text.getString(), password_edit_text.getString())
    }

    private fun loginWithFirebase(email: String, password: String) {
        mAuth.let {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            progress_bar.visibility = View.GONE
                            goToMain()
                        } else {
                            mAuth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(this) { task1 ->
                                        if (task1.isSuccessful) {
                                            //Save user in Firebase database
                                            if (mAuth.currentUser != null) {
                                                dbRef.child(USERS).child(splitEmail(mAuth.currentUser!!.email)).child(REQUEST).setValue(mAuth.currentUser!!.uid)

                                                goToMain()
                                            }
                                        } else {
                                            showMessage("There was an error logging in. Please verify your email and password.")
                                        }
                                        progress_bar.visibility = View.GONE
                                    }
                        }
                    }
        }
    }

    private fun goToMain() {
        val currentUser: FirebaseUser? = mAuth.currentUser

        if(currentUser != null) {
            val mainIntent = Intent(this, MainActivity::class.java)
            mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            mainIntent.putExtra(ARG_EMAIL, currentUser.email)
            mainIntent.putExtra(ARG_ID, currentUser.uid)

            startActivity(mainIntent)
            finish()
        }
    }
}

fun EditText.getString(): String = this.text.toString()
fun EditText.isBlank() = this.text.toString().isBlank()
fun EditText.isValidEmail() = !this.isBlank()
        && Patterns.EMAIL_ADDRESS.matcher(this.text.toString()).matches()
fun EditText.isValidPassword() = !this.isBlank() && this.text.toString().length >= 6
fun splitEmail(str: String?) : String? {
    if(str != null) {
        return str.split("@")[0]
    }
    return str
}

fun Activity.showMessage(msg:String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
