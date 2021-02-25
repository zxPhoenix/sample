package com.postindustria.ssai.common.monitoring

interface BaseMetric {
    fun initialize()
    fun flush()
    fun destroy()
}