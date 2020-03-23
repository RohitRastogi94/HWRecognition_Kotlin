package com.tarento.markreader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import com.tarento.markreader.data.ApiClient
import com.tarento.markreader.data.OCRService
import com.tarento.markreader.data.model.login.LoginRequest
import com.tarento.markreader.data.model.login.LoginResponse

import com.tarento.markreader.utils.ProgressBarUtil
import com.tarento.markreader.data.preference.AppPreferenceHelper
import kotlinx.android.synthetic.main.activity_login.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    companion object {
        val TAG = LoginActivity.javaClass.canonicalName
    }

    lateinit var usernameEditText: EditText
    lateinit var passwordEditText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            loginRequest(username, password)

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

    private fun loginRequest(username: String, password: String) {
        val apiInterface: OCRService = ApiClient.getClient()!!.create(OCRService::class.java)

        val loginRequest = LoginRequest(
            "rahul",
            "welcome"
        )
        Log.d(TAG, "request login() called with: data = [$loginRequest]")
        val hero = apiInterface.login(loginRequest)

        hero.enqueue(object : Callback<LoginResponse> {
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e(TAG, "onResponse: Failuer", t)
                Toast.makeText(this@LoginActivity, "Some thing went wrong", Toast.LENGTH_SHORT)
                    .show()
                ProgressBarUtil.dismissProgressDialog()
            }

            override fun onResponse(
                call: Call<LoginResponse>,
                response: Response<LoginResponse>
            ) {
                Log.d(TAG, "onResponse: ${response.isSuccessful}")
                if (response != null && response.isSuccessful && response.body() != null) {
                    ProgressBarUtil.dismissProgressDialog()
                    Log.d(TAG, "onResponse: ${response.body()}")

                    val loginResponse = response.body()

                    loginResponse?.let {
                        if (loginResponse.http.status == 200) {
                            var appPreferenceHelper = AppPreferenceHelper(applicationContext)
                            appPreferenceHelper.removePreference()
                            appPreferenceHelper.setSchoolCode(loginResponse.data.school.school_code)
                            appPreferenceHelper.setTeacherCode(loginResponse.data.teacher.teacher_code)
                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    JavaPreviewActivity::class.java
                                )
                            )
                            finish()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Some thing went wrong",
                                Toast.LENGTH_SHORT
                            )
                                .show()

                        }

                    }
                }
            }

        })

    }
}
