package com.postindustria.ssai.common.monitoring.gcp

import com.postindustria.ssai.common.monitoring.BaseMetric
import com.postindustria.ssai.common.monitoring.MetricsList
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

class GCPMonitoring : MetricsList {
    val metrics: MutableList<BaseMetric> = mutableListOf()
    @Autowired
    lateinit var masterPlaylistLatency: MasterPlaylistLatency


    @PostConstruct
    fun registerMetrics(){
        StackdriverStatsExporter.createAndRegister();

        registerMetric(masterPlaylistLatency)

        GlobalScope.launch{
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

    override fun flushMasterPlaylistLatency(value: Long) {
        masterPlaylistLatency.put(value)
    }

    @PreDestroy
    fun destroy() {
        metrics.forEach {
            destroyMetric(it)
        }
    }
}