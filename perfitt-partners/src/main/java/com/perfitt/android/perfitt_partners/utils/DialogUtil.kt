package com.perfitt.android.perfitt_partners.utils

import android.app.Activity
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.perfitt.android.perfitt_partners.R
import com.perfitt.android.perfitt_partners.views.CustomAlertDialogBuilder
import kotlinx.android.synthetic.main.dialog_progress_message.view.*


class DialogUtil {

    /**
     * 메세지 다이얼로그 보여주기
     */
    fun showMessageDialog(activity: Activity?, title: String = "", message: String, onConfirm: (() -> Unit)? = null, onCancel: (() -> Unit)? = null) {
        activity?.let {
            val dialog = CustomAlertDialogBuilder(activity).apply {
                setCustomTitle(title)
                setCustomMessage(message)
                setPositive(android.R.string.ok)
                onPositive {
                    dialog.dismiss()
                    onConfirm?.invoke()
                }
                if (onCancel != null) {
                    setNegative(R.string.term_cancel)
                    onNegative {
                        dialog.dismiss()
                        onCancel()
                    }
                } else {
                    goneNegative()
                }
            }.dialog

            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(false)
            dialog.show()
        }
    }

    /**
     * 메세지 다이얼로그 보여주기 ( OK, CANCEL 버튼 원하는 문구 집어 넣기 )
     */
    fun showMessageDialogCustomText(activity: Activity?, title: String = "", message: String = "", positiveText: String = "", negativeText: String = "", onConfirm: (() -> Unit)? = null, onCancel: (() -> Unit?)? = null) {
        activity?.let {
            val dialog = CustomAlertDialogBuilder(activity).apply {
                setCustomTitle(title)
                setCustomMessage(message)
                setPositive(positiveText)
                onPositive {
                    dialog.dismiss()
                    onConfirm?.invoke()
                }
                if (onCancel != null) {
                    setNegative(negativeText)
                    onNegative {
                        dialog.dismiss()
                        onCancel.invoke()
                    }
                } else {
                    goneNegative()
                }

            }.dialog

            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
    }

    /**
     * 메시지 프로그래스 다이얼로그
     */
    fun showProgressMessage(activity: Activity?, message: String): AlertDialog? {
        activity?.let {
            LayoutInflater.from(activity).inflate(R.layout.dialog_progress_message, null, false).run {
                txt_message.text = message
                val dialog = CustomAlertDialogBuilder(activity, this).dialog
                dialog.setCanceledOnTouchOutside(false)
                dialog.setCancelable(false)
                dialog.show()
                return dialog
            }
        }
        return null
    }

    private object Holder {
        val INSTANCE = DialogUtil()
    }

    companion object {
        val instance: DialogUtil by lazy { Holder.INSTANCE }
    }
}