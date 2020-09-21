package com.perfitt.android.perfitt_partners.utils

import android.content.Context

class PreferenceUtil(context: Context) : BasePreferenceUtil(context) {

    /**
     * 앱 서비스 소개 최초 보여줬는지 여부
     */
    val isFirstAppTutorial: Boolean
        get() = get(FIRST_APP_TUTORIAL, false)

    /**
     * 앱 서비스 소개 1회 보여줬는지 여부 저장
     */
    fun setFirstAppTutorial(isPushTutorial: Boolean) {
        put(FIRST_APP_TUTORIAL, isPushTutorial)
    }

    companion object {
        fun instance(context: Context): PreferenceUtil {
            return PreferenceUtil(context)
        }

        private const val FIRST_APP_TUTORIAL = "first_app_tutorial"
    }
}