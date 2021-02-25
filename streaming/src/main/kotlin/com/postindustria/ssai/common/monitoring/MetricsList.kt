package com.postindustria.ssai.common.monitoring

interface MetricsList {
    fun flushMasterPlLatency(value: Long)
    fun flushMediaPlLatency(value: Long)
    fun flushTSChunkLatency(value: Long)
    fun flushTSChunkDownloadingLatency(value: Long)
    fun flushTSChunkResponseLatency(value: Long)
    fun flushStreamInitkLatency(value: Long)
}