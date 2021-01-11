package com.perfitt.android.perfitt_partners.models

class ResponseFeet {
    var id: String = ""
    var feet: FeetModel? = null

    inner class FeetModel {
        var left: FootModel? = null
        var right: FootModel? = null

        inner class FootModel {
            var uri: String = ""
            var length: Double? = 0.0
            var width: Double? = 0.0
        }
    }
}