package com.perfitt.android.perfitt_partners.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.perfitt.android.perfitt_partners.R
import com.perfitt.android.perfitt_partners.controller.APIController
import com.perfitt.android.perfitt_partners.controller.PerfittPartners
import com.perfitt.android.perfitt_partners.models.ResponseFeet
import kotlinx.android.synthetic.main.activity_foot_result.*
import kotlinx.android.synthetic.main.content_home_foot_data.*
import kotlinx.android.synthetic.main.content_options.*
import org.json.JSONObject

class FootResultActivity : AppCompatActivity() {
    private var feetId = ""
    private var parentType = ""
    private var type = 0
    var currentZoom: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_foot_result)
        onNewIntent(intent)

        btn_retake.setOnClickListener {
            if (parentType == LandingActivity.A4) {
                startActivity(Intent(this, A4DetectorActivity::class.java).apply {
                    putExtra("type", this.type)
                    putExtra("currentZoom", currentZoom)
                })
            } else {
                startActivity(Intent(this, KitDetectorActivity::class.java).apply {
                    putExtra("type", type)
                    putExtra("currentZoom", currentZoom)
                })
            }
            finish()
        }

        btn_next.setOnClickListener {
            val gender = if (radio_male.isChecked) "M" else if (radio_female.isChecked) "F" else ""
            val nickName = edit_name.text.toString()
            val handler = Handler()
            progress_bar.visibility = View.VISIBLE
            Thread(Runnable {
                val response = APIController.instance.requestCreateUsers(
                    PerfittPartners.API_KEY,
                    feetId,
                    PerfittPartners.AVERAGE_SIZE,
                    nickName,
                    gender,
                    PerfittPartners.CUSTOMER_ID
                ) { errorCode, errorType, errorMessage ->
                    handler.post {
                        progress_bar.visibility = View.GONE
                        Toast.makeText(this@FootResultActivity, "errorCode:$errorCode \n errorMessage$errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
                response?.let { response ->
                    handler.post {
                        progress_bar.visibility = View.GONE
                        Log.d("Dony", "response:$response")
                        JSONObject(response).getString("id").let { id ->
                            PerfittPartners.instance.confirmListener?.onConfirm("javascript:PERFITT_CALLBACK('$id')")
                            finish()
                        }
                    }
                }
            }).start()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        parentType = intent?.getStringExtra("parentType") ?: ""
        intent?.getIntExtra("type", 0)?.let { type ->
            this.type = type
        }
        intent?.getIntExtra("currentZoom", 0)?.let { zoom ->
            currentZoom = zoom
        }
        intent?.getStringExtra("response")?.let { response ->
            Gson().fromJson(response, ResponseFeet::class.java).run {
                feetId = id
                feet?.run {
                    left?.run {
                        txt_start_length.text = String.format(getString(R.string.term_mm), length?.toInt().toString())
                        txt_start_width.text = String.format(getString(R.string.term_mm), width?.toInt().toString())
                    }
                    right?.run {
                        txt_end_length.text = String.format(getString(R.string.term_mm), length?.toInt().toString())
                        txt_end_width.text = String.format(getString(R.string.term_mm), width?.toInt().toString())
                    }
                }
            }
        }
    }
}