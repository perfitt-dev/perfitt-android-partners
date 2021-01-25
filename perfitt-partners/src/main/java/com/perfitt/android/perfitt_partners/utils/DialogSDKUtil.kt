package com.perfitt.android.perfitt_partners.utils

import android.app.Activity
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.perfitt.android.perfitt_partners.R
import com.perfitt.android.perfitt_partners.views.CustomAlertDialogBuilder
import kotlinx.android.synthetic.main.sdk_dialog_progress_message.view.*
import kotlinx.android.synthetic.main.sdk_dialog_sdk_size_picker.view.*
import kotlinx.android.synthetic.main.sdk_dialog_update_foot_profile.view.*


class DialogSDKUtil {

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
                    setNegative(R.string.sdk_term_cancel)
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
    fun showMessageDialogCustomText(
        activity: Activity?,
        title: String = "",
        message: String = "",
        positiveText: String = "",
        negativeText: String = "",
        onConfirm: (() -> Unit)? = null,
        onCancel: (() -> Unit?)? = null
    ) {
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
            LayoutInflater.from(activity).inflate(R.layout.sdk_dialog_progress_message, null, false).run {
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


    /**
     * 내 발 정보 편집하기
     */
    fun showUpdateFootProfile(activity: Activity?, name: String?, gender: String?, averageSize: Int?, updateUnit: (name: String?, gender: String?, averageSize: Int) -> Unit) {
        activity?.let {
            LayoutInflater.from(activity).inflate(R.layout.sdk_dialog_update_foot_profile, null, false).run {
                CustomAlertDialogBuilder(activity, this).apply {
                    setCustomTitle(R.string.sdk_msg_dialog_update_foot_profile_title)
                    setPositive(R.string.sdk_term_ok)
                    setNegative(R.string.sdk_term_cancel)

                    name?.let {
                        edit_name.setText(it)
                    }

                    gender?.let {
                        if (gender.isNotEmpty()) {
                            if (gender == "M") radio_male.isChecked = true else radio_female.isChecked = true
                        }
                    }

                    averageSize?.let {
                        btn_size.text = averageSize.toString()
                    }

                    btn_size.setOnClickListener {
                        showSizePicker(activity, btn_size.text.toString()) { size ->
                            btn_size.text = size
                        }
                    }

                    onPositive {
                        val pName = edit_name.text.toString()
                        val pGender = if (radio_male.isChecked) "M" else if (radio_female.isChecked) "F" else null
                        val pAverageSize = if (btn_size.text.toString().isNotEmpty()) {
                            btn_size.text.toString().toInt()
                        } else {
                            null
                        }
                        if (pAverageSize != null) {
                            updateUnit(pName, pGender, pAverageSize)
                            dialog.dismiss()
                        } else {
                            Toast.makeText(context, context.getString(R.string.sdk_msg_dialog_empty_avg_size), Toast.LENGTH_SHORT).show()
                        }
                    }
                    onNegative {
                        dialog.dismiss()
                    }
                }.dialog.run {
                    show()
                }
            }
        }
    }

    /**
     * 사이즈 선택 다이얼로그
     */
    fun showSizePicker(activity: Activity?, size: String, onPositive: (size: String) -> Unit) {
        activity?.let {
            val sizes = activity.resources.getStringArray(R.array.size)
            LayoutInflater.from(activity).inflate(R.layout.sdk_dialog_sdk_size_picker, null, false).run {
                CustomAlertDialogBuilder(activity, this).apply {
                    number_picker.run {
                        minValue = 0
                        maxValue = sizes.size - 1
                        wrapSelectorWheel = false
                        displayedValues = sizes
                        value = if (size.isEmpty()) 0 else {
                            // 선택된 사이즈 포지션 가져오기
                            var result = 0
                            sizes.forEachIndexed { index, forSize ->
                                if (size == forSize)
                                    result = index
                            }
                            result
                        }
                    }
                    goneNegative()
                    setPositive(R.string.sdk_term_ok)
                    onPositive {
                        onPositive.invoke(sizes[number_picker.value])
                        dialog.dismiss()
                    }
                }.dialog.run {
                    show()
                }
            }
        }
    }

    private object Holder {
        val INSTANCE = DialogSDKUtil()
    }

    companion object {
        val INSTANCE: DialogSDKUtil by lazy { Holder.INSTANCE }
    }
}