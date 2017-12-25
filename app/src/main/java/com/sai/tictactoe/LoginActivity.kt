package com.sai.tictactoe

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private val TAG = LoginActivity::class.java.simpleName

    private lateinit var mAuth: FirebaseAuth

    private var mCurrentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
    }

    override fun onStart() {
        super.onStart()
        mCurrentUser = mAuth.currentUser
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
                            showMessage("Login successful")
                        } else {
                            showMessage("Login failed" + task.exception)
                        }
                    }
        }
    }
}

private fun EditText.getString(): String = this.text.toString()
private fun EditText.isBlank() = this.text.toString().isBlank()
private fun EditText.isValidEmail() = !this.isBlank()
        && Patterns.EMAIL_ADDRESS.matcher(this.text.toString()).matches()
private fun EditText.isValidPassword() = !this.isBlank() && this.text.toString().length >= 6

fun Activity.showMessage(msg:String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
