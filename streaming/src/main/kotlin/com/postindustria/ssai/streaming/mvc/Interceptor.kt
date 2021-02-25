package com.postindustria.ssai.streaming.mvc

import com.postindustria.ssai.common.model.log
import com.postindustria.ssai.common.monitoring.Monitoring
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class Interceptor : HandlerInterceptorAdapter() {
    @Autowired
    lateinit var monitoring: Monitoring

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val startTime = System.currentTimeMillis()
        request.setAttribute("startTime", startTime)

        return super.preHandle(request, response, handler)
    }

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        super.afterCompletion(request, response, handler, ex)
        val startTime = request.getAttribute("startTime") as Long
        val endTime = System.currentTimeMillis()
        val timeInMillis = endTime - startTime

        with(request.requestURL) {
            when {
                contains("/play/" ) -> {
                    monitoring.flushStreamInitkLatency(timeInMillis)
                }
                contains("master.m3u8") -> {
                    monitoring.flushMasterPlLatency(timeInMillis)
                }
                contains(".m3u8") -> {
                    monitoring.flushMediaPlLatency(timeInMillis)
                }
                contains(".ts") -> {
                    monitoring.flushTSChunkLatency(timeInMillis)
                }
            }
        }
    }
}