package com.tarento.markreader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    lateinit var usernameEditText: EditText
    lateinit var passwordEditText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById<EditText>(R.id.username)
        passwordEditText = findViewById<EditText>(R.id.password)

        password.apply {
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginDataChanged(
                            usernameEditText.text.toString(),
                            passwordEditText.text.toString()
                        )
                }
                false
            }
        }

        buttonLogin.setOnClickListener {
            loginDataChanged(usernameEditText.text.toString(), passwordEditText.text.toString())
        }

        cancelId.setOnClickListener {
            this.finish()
        }
    }

    private fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            usernameEditText.error = getString(R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            passwordEditText.error = getString(R.string.invalid_password)
        } else {
            loading.visibility = View.VISIBLE
            startActivity(Intent(this@LoginActivity, JavaPreviewActivity::class.java))
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
    }

    private fun isUserNameValid(username: String): Boolean {
        /* return if (username.contains('@')) {
             Patterns.EMAIL_ADDRESS.matcher(username).matches()
         } else {
             username.isNotBlank()
         }*/
        return username.isNotBlank()
    }

    private fun isPasswordValid(password: String): Boolean {
        //return password.length > 5
        return password.isNotBlank()
    }
}
