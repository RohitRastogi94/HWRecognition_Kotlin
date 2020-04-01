package com.tarento.markreader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.google.gson.Gson
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
        val TAG = LoginActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        password.apply {
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginDataChanged(username.text.toString(), password.text.toString())
                }
                false
            }
        }

        buttonLogin.setOnClickListener {
            loginDataChanged(username.text.toString(), password.text.toString())
        }

        cancelId.setOnClickListener {
            this.finish()
        }
    }

    private fun loginDataChanged(usernameValue: String, passwordValue: String) {
        if (!isUserNameValid(usernameValue)) {
            username.error = getString(R.string.invalid_username)
        } else if (!isPasswordValid(passwordValue)) {
            password.error = getString(R.string.invalid_password)
        } else {
            loading.visibility = View.VISIBLE
            loginRequest(usernameValue, passwordValue)

        }
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
        val apiInterface: OCRService? = ApiClient.createAPIService()
        val loginRequest = LoginRequest(username, password)
        Log.d(TAG, "request login() called with: data = [$loginRequest]")
        val loginAPICall = apiInterface?.login(loginRequest)

        loginAPICall?.enqueue(object : Callback<LoginResponse> {
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e(TAG, "onResponse: Failuer", t)
                Toast.makeText(
                    this@LoginActivity, getString(R.string.something_went_wrong),
                    Toast.LENGTH_SHORT
                ).show()
                ProgressBarUtil.dismissProgressDialog()
                loading.visibility = View.GONE
            }

            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                Log.d(TAG, "onResponse: ${response.isSuccessful}")
                ProgressBarUtil.dismissProgressDialog()
                loading.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "onResponse: ${response.body()}")
                    val loginResponse = response.body()
                    loginResponse?.let {
                        if (loginResponse.http.status == 200) {
                            val appPreferenceHelper = AppPreferenceHelper(applicationContext)
                            appPreferenceHelper.removePreference()
                            appPreferenceHelper.setSchoolCode(loginResponse.data.school.school_code)
                            appPreferenceHelper.setTeacherCode(loginResponse.data.teacher.teacher_code)

                            val intent = Intent(this@LoginActivity, IndexActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@LoginActivity, getString(R.string.something_went_wrong),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    var errorMessage = getString(R.string.invalid_credentials)
                    try {
                        val loginErrorResponseOj = Gson().fromJson<LoginResponse>(
                            response.errorBody()?.string() ?: "",
                            LoginResponse::class.java
                        )
                        loginErrorResponseOj?.let {
                            errorMessage = it.why
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    Toast.makeText(
                        this@LoginActivity,
                        errorMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }
}
