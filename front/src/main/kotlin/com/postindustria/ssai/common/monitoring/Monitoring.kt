package com.postindustria.ssai.common.monitoring

import com.postindustria.ssai.common.monitoring.gcp.GCPMonitoring
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Monitoring : MetricsList {
    @Autowired
    lateinit var implementation: GCPMonitoring

    override fun flushMasterPlaylistLatency(value: Long) {
        implementation.flushMasterPlaylistLatency(value)
    }
}