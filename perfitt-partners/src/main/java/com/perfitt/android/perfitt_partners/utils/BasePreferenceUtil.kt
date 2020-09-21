package com.perfitt.android.perfitt_partners.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager


open class BasePreferenceUtil(context: Context) {
    private var mPreference: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * key 수동 설정
     *
     * @param key   키 값
     * @param value 내용
     */
    protected fun put(key: String, value: String) {
        val editor = mPreference.edit()
        editor.putString(key, value)
        editor.apply()
    }


    /**
     * String 값 가져오기
     *
     * @param key 키 값
     * @return String (기본값 null)
     */
    operator fun get(key: String): String? {
        return mPreference.getString(key, null)
    }

    /**
     * key 설정
     *
     * @param key   키 값
     * @param value 내용
     */
    protected fun put(key: String, value: Boolean) {
        val editor = mPreference.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    /**
     * Boolean 값 가져오기
     *
     * @param key   키 값
     * @param value 기본값
     * @return Boolean
     */
    operator fun get(key: String, value: Boolean): Boolean {
        return mPreference.getBoolean(key, value)
    }


    /**
     * key 설정
     *
     * @param key   키 값
     * @param value 내용
     */
    protected fun put(key: String, value: Int) {
        val editor = mPreference.edit()
        editor.putInt(key, value)
        editor.apply()
    }


    /**
     * int 값 가져오기
     *
     * @param key      키 값
     * @param defValue 기본값
     */
    operator fun get(key: String, defValue: Int): Int {
        return mPreference.getInt(key, defValue)
    }
}
