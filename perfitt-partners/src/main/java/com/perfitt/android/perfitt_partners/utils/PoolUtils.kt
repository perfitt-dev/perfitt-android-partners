package com.perfitt.android.perfitt_partners.utils

import com.perfitt.android.perfitt_partners.models.TFMappingModel

class PoolUtils {
    var leftFoot = FootRectLocation()
    var rightFoot = FootRectLocation()

    class FootRectLocation {
        var baseModel: TFMappingModel? = null
        var footModel: TFMappingModel? = null
        var leftTriModel: TFMappingModel? = null
        var rightTriModel: TFMappingModel? = null
    }

    fun clearLeftLocations() {
        leftFoot = FootRectLocation()
    }

    fun clearRightLocations() {
        rightFoot = FootRectLocation()
    }

    private object Holder {
        val INSTANCE = PoolUtils()
    }

    companion object {
        val instance: PoolUtils by lazy { Holder.INSTANCE }
    }
}