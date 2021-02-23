package com.perfitt.android.perfitt_partners.views

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.perfitt.android.perfitt_partners.R


class FontableNumberPicker : NumberPicker {

    constructor(context: Context) : super(context) {
        setColor()
        setFont()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setColor()
        setFont()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setColor()
        setFont()
    }

    private fun setColor() = try {
        NumberPicker::class.java.declaredFields.let { fields ->
//            fields.forEach {
//                if (it.name == "mSelectionDivider") {
//                    it.isAccessible = true
//                    it.set(this, ColorDrawable(ContextCompat.getColor(context, R.color.sdk_color_primary)))
//                }
//            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    /**
     * SP 로 부터 Pixel 값을 가져오기
     */
    private fun getPixelBySP(context: Context, sp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics).toInt()
    }

    private fun setFont() = try {
        NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint").let { field ->
            field.isAccessible = true
            //클래스의 휠 Paint 객체를 꺼내 폰트를 적용
            (field.get(this) as Paint).run {
                typeface = getTypeFace(R.font.barlow_medium)
                textSize = getPixelBySP(context, 19.5f).toFloat()
            }

            //선택된 숫자를 보여주는 TextView
            for (i in 0 until this.childCount) {
                //자식뷰로 꺼내 폰트를 적용
                this.getChildAt(i).run {
                    if (this is TextView) {
                        typeface = getTypeFace(R.font.barlow_medium)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 19.5f)
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    private fun getTypeFace(resId: Int): Typeface? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                resources.getFont(resId)
            } catch (e: Exception) {
                ResourcesCompat.getFont(context, resId)
            }
        } else {
            ResourcesCompat.getFont(context, resId)
        }
    }
}