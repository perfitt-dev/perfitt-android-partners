package com.perfitt.android.perfitt_partners.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.perfitt.android.perfitt_partners.R

class PermissionUtil {

    private var message: String? = null
    private var permissions: ArrayList<String>? = null
    private var grantedUnit: (() -> Unit)? = null
    private var deniedUnit: (() -> Unit)? = null
    private var systemPermissionSettingsUnit: (() -> Unit)? = null
    private fun clear() {
        message = null
        permissions = null
        grantedUnit = null
    }

    /**
     * 퍼미션 요청 메시지
     */
    fun setMessage(message: String): PermissionUtil {
        this.message = message
        return instance
    }

    /**
     * 퍼미션 설정
     */
    fun setPermissions(permissions: ArrayList<String>): PermissionUtil {
        this.permissions = permissions
        return instance
    }

    /**
     * 퍼미션 수락시 호출되는 Unit 설정
     */
    fun onGranted(grantedUnit: () -> Unit): PermissionUtil {
        this.grantedUnit = {
            grantedUnit.invoke()
            clear()
        }
        return instance
    }

    /**
     * 퍼미션 거절시 호출되는 Unit 설정
     */
    fun onDenied(deniedUnit: () -> Unit): PermissionUtil {
        this.deniedUnit = {
            deniedUnit.invoke()
            clear()
        }
        return instance
    }

    /**
     * 퍼미션 거절시 다이얼로그 셋팅 버튼 클릭시 호출되는 Unit 설정
     */
    fun onSystemPermissionSettings(settingsUnit: () -> Unit): PermissionUtil {
        this.systemPermissionSettingsUnit = {
            settingsUnit.invoke()
            clear()
        }
        return instance
    }

    /**
     * 퍼미션 체크하기
     */
    fun checkToPermissions(activity: Activity) {
        val deniedPermissions = ArrayList<String>()
        if (permissions == null) {
            return
        }

        /**
         * 퍼미션 요청하기
         */
        fun requestPermissions(permissions: Array<String>) {
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_PERMISSION)
        }

        permissions?.run {
            forEach {
                if (isDenied(activity, it)) {
                    // 수락되지 않은 퍼미션 찾기
                    deniedPermissions.add(it)
                }
            }

            if (deniedPermissions.isNotEmpty()) {
                // 수락되지 않은 퍼미션이 있다면
                val grantedMessage = message ?: ""

                if (grantedMessage.isEmpty()) {
                    // 다이얼로그 설정을 안했다면
                    requestPermissions(deniedPermissions.toTypedArray())
                    return
                }
                if (!activity.isFinishing) {
                    DialogUtil.instance.showMessageDialog(
                            activity,
                            title = getString(activity, R.string.msg_permission_title),
                            message = grantedMessage,
                            onConfirm = {
                                requestPermissions(deniedPermissions.toTypedArray())
                            }
                    )
                } else {
                    // 아무것도 하지 않는다.
                }
            } else {
                // 퍼미션이 수락되어 있다면
                grantedUnit?.invoke()
            }
        }
    }

    /**
     * 퍼미션 거절
     */
    private fun isDenied(activity: Activity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED
    }

    /**
     * 거절된 퍼미션 설정 화면 보내기
     */
    fun deniedPermissionSettings(activity: Activity, permissions: Array<out String>) {
        val grantedMessage = message ?: ""
        permissions.forEach {
            if (isDenied(activity, it)) {
                // 거절된 퍼미션이 존재하면
                DialogUtil.instance.showMessageDialogCustomText(
                        activity,
                        title = getString(activity, R.string.msg_permission_title),
                        message = if (grantedMessage.isNotEmpty()) {
                            grantedMessage + "\n\n" + getString(activity, R.string.msg_permission_denied_message)
                        } else {
                            // 다이얼로그 설정을 안했다면
                            getString(activity, R.string.msg_permission_denied_message)
                        },
                        positiveText = getString(activity, R.string.term_settings),
                        negativeText = getString(activity, R.string.term_close),
                        onConfirm = {
                            systemPermissionSettingsUnit?.invoke()
                            activity.startActivityForResult(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:" + activity.packageName)
                            }, REQUEST_CODE_PERMISSION)
                        },
                        onCancel = {
                            deniedUnit?.invoke()
                        }
                )
                clear()
                return
            }
        }
        grantedUnit?.invoke()
    }

    private fun getString(context: Context, @StringRes stringRes: Int): String {
        return context.getString(stringRes)
    }


    private object Holder {
        val INSTANCE = PermissionUtil()
    }

    companion object {
        val instance: PermissionUtil by lazy { Holder.INSTANCE }
        const val REQUEST_CODE_PERMISSION = 22001
    }
}