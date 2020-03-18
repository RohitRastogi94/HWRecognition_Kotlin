package com.tarento.markreader.data.preference

interface PreferenceHelper {
    fun setSchoolCode(schoolCode:String)

    fun setTeacherCode(teacherCode:String)

    fun getSchoolCode(): String?

    fun getTeacherCode():String?

    fun removePreference()


}