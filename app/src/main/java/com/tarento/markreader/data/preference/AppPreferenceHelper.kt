package com.tarento.markreader.data.preference

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.tarento.markreader.data.model.FetchExamsResponse

class AppPreferenceHelper constructor(context: Context) : PreferenceHelper {

    companion object {
        const val PREF_NAME = "com.tarento.markreader"
        const val KEY_SCHOOL_CODE = "com.tarento.markreader.SCHOOL_CODE"
        const val KEY_TEACHER_CODE = "com.tarento.markreader.TEACHER_CODE"
        const val KEY_STUDENT_CODE = "com.tarento.markreader.STUDENT_CODE"
        const val KEY_EXAM_CODE = "com.tarento.markreader.EXAM_CODE"
        const val KEY_EXAM_DATE = "com.tarento.markreader.EXAM_DATE"
        const val KEY_EXAM_CODE_LIST = "com.tarento.markreader.EXAM_CODE_LIST"
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

    override fun clearStudentDetails() {
        setExamDate(null)
        setStudentCode(null)
        setExamCode(null)
        setExamCodeList(null)

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

    override fun setStudentCode(studentCode: String?) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_STUDENT_CODE, studentCode)
        editor.apply()
    }

    override fun setExamCode(examCode: String?) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_EXAM_CODE, examCode)
        editor.apply()
    }

    override fun setExamDate(examDate: String?) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_EXAM_DATE, examDate)
        editor.apply()
    }

    override fun getSchoolCode(): String? {
        return sharedPreferences.getString(KEY_SCHOOL_CODE, null)
    }

    override fun getTeacherCode(): String? {
        return sharedPreferences.getString(KEY_TEACHER_CODE, null)
    }

    override fun getStudentCode(): String? {
        return sharedPreferences.getString(KEY_STUDENT_CODE, null)
    }

    override fun getExamCode(): String? {
        return sharedPreferences.getString(KEY_EXAM_CODE, null)
    }

    override fun getExamDate(): String? {
        return sharedPreferences.getString(KEY_EXAM_DATE, null)
    }

    override fun setExamCodeList(fetchExamsResponse: FetchExamsResponse?) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_EXAM_CODE_LIST, Gson().toJson(fetchExamsResponse).toString())
        editor.apply()
    }

    override fun getExamCodeList(): FetchExamsResponse? {
        return Gson().fromJson(sharedPreferences.getString(KEY_EXAM_CODE_LIST, null), FetchExamsResponse::class.java)
    }

}