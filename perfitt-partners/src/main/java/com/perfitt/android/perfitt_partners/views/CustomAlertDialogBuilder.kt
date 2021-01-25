package com.perfitt.android.perfitt_partners.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.perfitt.android.perfitt_partners.R

/**
 * Created by Dony on 2017. 12. 15..
 */
class CustomAlertDialogBuilder : AlertDialog.Builder {
    private var layoutTitle: View? = null
    private var layoutMessage: View? = null
    private var txtTitle: TextView? = null
    private var txtMessage: TextView? = null
    private var layoutButton: View? = null
    private var btnPositive: Button? = null
    private var btnNegative: Button? = null
    var dialog: AlertDialog
        private set

    @SuppressLint("InflateParams")
    constructor(activity: Activity) : super(activity) {
        dialog = setView(LayoutInflater.from(activity).inflate(R.layout.sdk_dialog_basic, null, false).apply {
            layoutTitle = findViewById(R.id.layout_title)
            layoutMessage = findViewById(R.id.layout_message)
            txtTitle = findViewById(R.id.txt_title)
            txtMessage = findViewById(R.id.txt_message)
            layoutButton = findViewById(R.id.layout_button)
            btnPositive = findViewById(R.id.btn_positive)
            btnNegative = findViewById(R.id.btn_negative)
        }).create()
    }

    constructor(context: Context, view: View) : super(context) {
        dialog = setView(view).create().apply {
            layoutTitle = view.findViewById(R.id.layout_title)
            layoutMessage = view.findViewById(R.id.layout_message)
            txtTitle = view.findViewById(R.id.txt_title)
            txtMessage = view.findViewById(R.id.txt_message)
            layoutButton = view.findViewById(R.id.layout_button)
            btnPositive = view.findViewById(R.id.btn_positive)
            btnNegative = view.findViewById(R.id.btn_negative)
        }
    }

    /**
     * 타이틀 설정
     *
     * @param title title
     */
    fun setCustomTitle(title: String) {
        if (title.isEmpty()) {
            layoutTitle?.visibility = View.GONE
        } else {
            layoutTitle?.visibility = View.VISIBLE
            txtTitle?.text = title
        }
    }

    /**
     * 타이틀 설정 (리소스 아이디)
     *
     * @param resId resId
     */
    fun setCustomTitle(resId: Int) {
        txtTitle?.setText(resId)
    }

    /**
     * 메시지 설정
     *
     * @param message message
     */
    fun setCustomMessage(message: String) {
        if (message.isEmpty()) {
            txtMessage?.visibility = View.GONE
        } else {
            txtMessage?.visibility = View.VISIBLE
            txtMessage?.text = message
        }
    }

    /**
     * 메시지 설정(리소스 아이디)
     *
     * @param resId resId
     */
    fun setCustomMessage(resId: Int) {
        txtMessage?.setText(resId)
    }

    /**
     * 메시지 설정
     *
     * @param message message
     */
    fun setCustomMessage(message: Spanned) {
        txtMessage?.text = message
    }

    /**
     * 버튼 타이틀 설정
     *
     * @param title title
     */
    fun setPositive(title: String) {
        btnPositive?.text = title
    }

    /**
     * 버튼 타이틀 설정(리소스 아이디)
     *
     * @param resId resId
     */
    fun setPositive(resId: Int) {
        btnPositive?.setText(resId)
    }


    /**
     * 버튼 타이틀 설정
     *
     * @param title title
     */
    fun setNegative(title: String) {
        btnNegative?.text = title
    }

    /**
     * 버튼 타이틀 설정(리소스 아이디)
     *
     * @param resId resId
     */
    fun setNegative(resId: Int) {
        btnNegative?.setText(resId)
    }

    /**
     * 버튼 리스너 설정
     *
     * @param onClickListener onClickListener
     */
    fun onPositive(onClickListener: (Any) -> Unit) {
        btnPositive?.setOnClickListener(onClickListener)
    }

    /**
     * 버튼 텍스트 컬러 설정
     */
    fun setPositiveTextColor(resId: Int) {
        btnPositive?.setTextColor(ContextCompat.getColor(context, resId))
    }

    /**
     * 버튼 텍스트 컬러 설정
     */
    fun setNegativeTextColor(resId: Int) {
        btnNegative?.setTextColor(ContextCompat.getColor(context, resId))
    }

    /**
     * 버튼 리스너 설정
     *
     * @param onClickListener onClickListener
     */
    fun onNegative(onClickListener: (Any) -> Unit) {
        btnNegative?.setOnClickListener(onClickListener)
    }

    /**
     * 버튼 배경 설정
     */
    fun setNegativeDrawable(resId: Int) {
        btnNegative?.setBackgroundResource(resId)
    }

    /**
     * 버튼 배경 설정
     */
    fun setPositiveDrawable(resId: Int) {
        btnPositive?.setBackgroundResource(resId)
    }

    /**
     * 버튼 숨기기
     */
    fun goneNegative() {
        btnNegative?.visibility = View.GONE
    }

    /**
     * 버튼 레이아웃 숨기기
     */
    fun goneLayoutButton() {
        layoutButton?.visibility = View.GONE
    }
}
