package com.tarento.markreader.data.preference

import com.tarento.markreader.data.model.FetchExamsResponse

interface PreferenceHelper {
    fun setSchoolCode(schoolCode:String)

    fun setTeacherCode(teacherCode:String)

    fun setStudentCode(studentCode:String?)

    fun setExamCode(examCode:String?)

    fun setExamDate(examDate:String?)

    fun getSchoolCode(): String?

    fun getTeacherCode():String?

    fun getStudentCode():String?

    fun getExamCode():String?

    fun getExamDate():String?

    fun setExamCodeList(fetchExamsResponse: FetchExamsResponse?)

    fun getExamCodeList():FetchExamsResponse?

    fun removePreference()

    fun clearStudentDetails()

}