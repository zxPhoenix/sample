package com.postindustria.ssai.common.monitoring

interface MetricsList {
    fun flushMasterPlaylistLatency(value: Long)
}