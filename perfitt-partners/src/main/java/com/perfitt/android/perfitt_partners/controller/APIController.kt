package com.perfitt.android.perfitt_partners.controller

import android.content.ContentValues
import android.util.Log
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class APIController {

    private fun requestGet(_url: String?, _params: ContentValues?): String? {
        var urlConn: HttpURLConnection? = null

        // URL 뒤에 붙여서 보낼 파라미터.
        val sbParams = StringBuffer()
        /**
         * 1. StringBuffer에 파라미터 연결
         */
        // 보낼 데이터가 없으면 파라미터를 비운다.
        if (_params == null) sbParams.append("") else {
            // 파라미터가 2개 이상이면 파라미터 연결에 &가 필요하므로 스위칭할 변수 생성.
            var isAnd = false
            // 파라미터 키와 값.
            var key: String
            var value: String
            for ((key1, value1) in _params.valueSet()) {
                key = key1
                value = value1.toString()

                // 파라미터가 두개 이상일때, 파라미터 사이에 &를 붙인다.
                if (isAnd) sbParams.append("&")
                sbParams.append(key).append("=").append(value)
                // 파라미터가 2개 이상이면 isAnd를 true로 바꾸고 다음 루프부터 &를 붙인다.
                if (!isAnd) if (_params.size() >= 2) isAnd = true
            }
        }
        /**
         * 2. HttpURLConnection을 통해 web의 데이터를 가져온다.
         */
        try {
            val url = URL(_url)
            urlConn = url.openConnection() as HttpURLConnection

            // [2-1]. urlConn 설정.
            urlConn.run {
                readTimeout = 10000
                connectTimeout = 15000
                requestMethod = "GET" // URL 요청에 대한 메소드 설정 : GET/POST.
//                doOutput = true
                doInput = true
                setRequestProperty("Accept-Charset", "utf-8") // Accept-Charset 설정.
                setRequestProperty("Context-Type", "application/json")
            }

            // [2-2]. parameter 전달 및 데이터 읽어오기.
//            val pw = PrintWriter(OutputStreamWriter(urlConn.outputStream))
//            pw.write(sbParams.toString())
//            pw.flush() // 출력 스트림을 flush. 버퍼링 된 모든 출력 바이트를 강제 실행.
//            pw.close() // 출력 스트림을 닫고 모든 시스템 자원을 해제.

            // [2-3]. 연결 요청 확인.
            // 실패 시 null을 리턴하고 메서드를 종료.
            Log.d("perfitt-partners", "responseCode: " + urlConn.responseCode)
            Log.d("perfitt-partners", "responseMessage: " + urlConn.responseMessage)
            if (urlConn.responseCode !== HttpURLConnection.HTTP_OK) return null

            // [2-4]. 읽어온 결과물 리턴.
            // 요청한 URL의 출력물을 BufferedReader로 받는다.
            val reader = BufferedReader(InputStreamReader(urlConn.inputStream, "UTF-8"))

            // 출력물의 라인과 그 합에 대한 변수.
            var line: String?
            var page: String? = ""

            // 라인을 받아와 합친다.
            while (reader.readLine().also { line = it } != null) {
                page += line
            }
            return page
        } catch (e: MalformedURLException) { // for URL.
            e.printStackTrace()
        } catch (e: IOException) { // for openConnection().
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            urlConn?.disconnect()
        }
        return null
    }

    private fun requestPost(_url: String?, _params: JSONObject?, errorUnit: ((errorCode: Int, errorMsg: String) -> Unit)): String? {
        var urlConn: HttpURLConnection? = null

        // URL 뒤에 붙여서 보낼 파라미터.
        val sbParams = StringBuffer()
        /**
         * 1. StringBuffer에 파라미터 연결
         */
        // 보낼 데이터가 없으면 파라미터를 비운다.
        if (_params == null) sbParams.append("") else {
            sbParams.append(_params.toString())
        }
        /**
         * 2. HttpURLConnection을 통해 web의 데이터를 가져온다.
         */
        try {
            val url = URL(_url)
            urlConn = url.openConnection() as HttpURLConnection

            // [2-1]. urlConn 설정.
            urlConn.run {
                readTimeout = 5000
                connectTimeout = 5000
                requestMethod = "POST" // URL 요청에 대한 메소드 설정 : GET/POST.
                doOutput = true
                doInput = true
                setRequestProperty("Accept-Charset", "utf-8") // Accept-Charset 설정.
                setRequestProperty("Context-Type", "application/json")
            }

            // [2-2]. parameter 전달 및 데이터 읽어오기.
            val pw = PrintWriter(OutputStreamWriter(urlConn.outputStream))
            pw.write(sbParams.toString())
            pw.flush() // 출력 스트림을 flush. 버퍼링 된 모든 출력 바이트를 강제 실행.
            pw.close() // 출력 스트림을 닫고 모든 시스템 자원을 해제.

            // [2-3]. 연결 요청 확인.
            // 실패 시 null을 리턴하고 메서드를 종료.
            if (urlConn.responseCode !== HttpURLConnection.HTTP_OK) {
                val errorReader = BufferedReader(InputStreamReader(urlConn.errorStream, "UTF-8"))
//                var line = ""
                val error = errorReader.readLine();
//                while (errorReader.readLine().also { line = it } != null) {
//                    error += line
//                    Log.d("Dony", "line:$line")
//                }
//                errorUnit(urlConn.responseCode, urlConn.responseMessage)
                Log.d("Dony", "error:$error")
                errorUnit(urlConn.responseCode, JSONObject(error).getString("message"))

                return null
            }

            // [2-4]. 읽어온 결과물 리턴.
            // 요청한 URL의 출력물을 BufferedReader로 받는다.
            val reader = BufferedReader(InputStreamReader(urlConn.inputStream, "UTF-8"))

            // 출력물의 라인과 그 합에 대한 변수.
            var line: String?
            var page: String? = ""

            // 라인을 받아와 합친다.
            while (reader.readLine().also { line = it } != null) {
                page += line
            }
            return page
        } catch (e: MalformedURLException) { // for URL.
            errorUnit(400, "MalformedURLException${e.toString()}")
            e.printStackTrace()
        } catch (e: IOException) { // for openConnection().
            errorUnit(400, "IOException${e.toString()}")
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
            errorUnit(400, "Exception${e.toString()}")
        } finally {
            urlConn?.disconnect()
        }
        return null
    }

    fun requestUsers(apiKey: String, leftImage: String, rightImage: String, sourceType: String, averageSize:Int, nickname:String?, gender:String?,
                     errorUnit: ((errorCode: Int, errorMsg: String) -> Unit)): String? {
        return requestPost("$USERS?apiKey=$apiKey", JSONObject().apply {
            put("leftImage", leftImage)
            put("rightImage", rightImage)
            put("sourceType", sourceType)
            put("averageSize", averageSize)
            put("nickname", nickname)
            put("gender", gender)
        }, errorUnit)
    }

    private object Holder {
        val INSTANCE = APIController()
    }

    companion object {
        val instance: APIController by lazy { Holder.INSTANCE }
        private const val BASE_URL = "https://dev-api.perfitt.io"
        private const val USERS = "$BASE_URL/core/v2/users"
        const val TUTORIAL_URL = "https://service.perfitt.io/howtomeasure"
    }
}