package com.tarento.markreader.data.preference

import android.content.Context
import android.content.SharedPreferences

class AppPreferenceHelper constructor(context: Context) : PreferenceHelper {

    companion object {
        const val PREF_NAME = "com.tarento.markreader"
        const val KEY_SCHOOL_CODE = "com.tarento.markreader.SCHOOL_CODE"
        const val KEY_TEACHER_CODE = "com.tarento.markreader.TEACHER_CODE"
    }

    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }


    private fun storeString(key: String, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun storeLong(key: String, value: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    private fun storeBoolean(key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }


    override fun removePreference() {
        sharedPreferences.edit().clear().apply()
    }

    override fun setSchoolCode(schoolCode: String) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_SCHOOL_CODE, schoolCode)
        editor.apply()
    }

    override fun setTeacherCode(teacherCode: String) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_TEACHER_CODE, teacherCode)
        editor.apply()
    }

    override fun getSchoolCode(): String? {
        return sharedPreferences.getString(KEY_SCHOOL_CODE, null)
    }

    override fun getTeacherCode(): String? {
        return sharedPreferences.getString(KEY_TEACHER_CODE, null)
    }


}