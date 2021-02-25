package com.postindustria.ssai.common.monitoring.gcp

import com.postindustria.ssai.common.monitoring.BaseMetric
import com.postindustria.ssai.common.monitoring.MetricsList
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

class GCPMonitoring : MetricsList {
    val metrics: MutableList<BaseMetric> = mutableListOf()
    @Autowired
    lateinit var masterPlaylistLatency: MasterPlaylistLatency
    @Autowired
    lateinit var mediaPlaylistLatency: MediaPlaylistLatency
    @Autowired
    lateinit var tsChunckLatency: TSChunkLatency
    @Autowired
    lateinit var tsChunckDownlaodLatency: TSChunkDownloadLatency
    @Autowired
    lateinit var tsChunckResponseLatency: TSChunkResponseLatency
    @Autowired
    lateinit var streamInitLatency: StreamInitLatency

    @PostConstruct
    fun registerMetrics(){
        StackdriverStatsExporter.createAndRegister();

        registerMetric(masterPlaylistLatency)
        registerMetric(mediaPlaylistLatency)
        registerMetric(tsChunckLatency)
        registerMetric(streamInitLatency)
        registerMetric(tsChunckDownlaodLatency)
        registerMetric(tsChunckResponseLatency)

        GlobalScope.launch {
            while (true) {
                metrics.forEach{
                    it.flush()
                }

                Thread.sleep(60 * 1000)
            }
        }
    }

    private fun registerMetric(metric: BaseMetric) {
        metrics.add(metric)
        metric.initialize()
    }

    private fun destroyMetric(metric: BaseMetric) {
        metric.destroy()
    }

    override fun flushMasterPlLatency(value: Long) {
        masterPlaylistLatency.put(value)
    }

    override fun flushMediaPlLatency(value: Long) {
        mediaPlaylistLatency.put(value)
    }

    override fun flushTSChunkLatency(value: Long) {
        tsChunckLatency.put(value)
    }

    override fun flushTSChunkDownloadingLatency(value: Long) {
        tsChunckDownlaodLatency.put(value)
    }

    override fun flushTSChunkResponseLatency(value: Long) {
        tsChunckResponseLatency.put(value)
    }

    override fun flushStreamInitkLatency(value: Long) {
        streamInitLatency.put(value)
    }

    @PreDestroy
    fun destroy() {
        metrics.forEach {
            destroyMetric(it)
        }
    }
}