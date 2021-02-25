package com.postindustria.ssai.common.monitoring

import com.postindustria.ssai.common.monitoring.gcp.GCPMonitoring
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Monitoring : MetricsList {
    @Autowired
    lateinit var implementation: GCPMonitoring

    override fun flushMasterPlLatency(value: Long) {
        run {
            implementation.flushMasterPlLatency(value)
        }
    }

    override fun flushMediaPlLatency(value: Long) {
        run {
            implementation.flushMediaPlLatency(value)
        }
    }

    override fun flushTSChunkLatency(value: Long) {
        run {
            implementation.flushTSChunkLatency(value)
        }
    }

    override fun flushTSChunkDownloadingLatency(value: Long) {
        run {
            implementation.flushTSChunkDownloadingLatency(value)
        }
    }

    override fun flushTSChunkResponseLatency(value: Long) {
        run {
            implementation.flushTSChunkResponseLatency(value)
        }
    }

    override fun flushStreamInitkLatency(value: Long) {
       run {
           implementation.flushStreamInitkLatency(value)
       }
    }

    private fun run(function: () -> Unit ) {
        GlobalScope.launch {
            function?.apply {
                this.invoke()
            }
        }
    }
}