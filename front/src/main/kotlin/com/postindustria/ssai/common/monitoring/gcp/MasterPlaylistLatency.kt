package com.postindustria.ssai.common.monitoring.gcp

import com.postindustria.ssai.common.monitoring.BaseMetric
import io.opencensus.stats.*
import io.opencensus.stats.Measure.MeasureLong
import org.springframework.stereotype.Component


@Component
class MasterPlaylistLatency : BaseMetric {

    private lateinit var measureMap: MeasureMap
    private val LATENCY_MS = MeasureLong.create(
            "front_get_master_playlist_latency",
            "The master playlist API call latency in milliseconds",
            "ms")

    private val LATENCY_BOUNDARIES = BucketBoundaries.create( listOf(0.0, 100.0, 200.0, 400.0, 1000.0, 2000.0, 4000.0))
    private val STATS_RECORDER: StatsRecorder = Stats.getStatsRecorder()

    override fun initialize() {
        measureMap = STATS_RECORDER.newMeasureMap();

        val view: View = View.create(
                View.Name.create("task_latency_distribution"),
                "The distribution of the task latencies.",
                LATENCY_MS,
                Aggregation.Distribution.create(LATENCY_BOUNDARIES),
                emptyList())

        val viewManager = Stats.getViewManager()
        viewManager.registerView(view)
    }

    override fun flush() {
        measureMap.record()
    }

    override fun destroy() {

    }

    fun put(value: Long) {
        measureMap.put(LATENCY_MS, value)
    }
}