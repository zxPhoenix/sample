package com.postindustria.ssai.streaming.mvc

import com.postindustria.ssai.common.model.SSAISession
import com.postindustria.ssai.common.model.log
import com.postindustria.ssai.common.monitoring.MeasureTools
import com.postindustria.ssai.common.monitoring.Monitoring
import com.postindustria.ssai.streaming.usecases.IdentificationService
import org.apache.catalina.connector.ClientAbortException
//import org.apache.catalina.connector.ClientAbortException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class SimpleCORSFilter: Filter {
    @Autowired
    lateinit var identificationService: IdentificationService
    @Autowired
    lateinit var _session: SSAISession

    @Autowired
    lateinit var monitoring: Monitoring

    override fun doFilter(req: ServletRequest?, res: ServletResponse?, chain: FilterChain?) {
        val request = req as HttpServletRequest
        val response = res as HttpServletResponse

        response.setHeader("Access-control-Allow-Origin", "*")
        response.setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE")
        response.setHeader("Access-Control-Allow-Headers", "*")
        response.setHeader("Access-Control-Max-Age", "3600")
        response.setHeader("Access-Control-Allow-Credentials", "true")
        response.setHeader("Keep-Alive", "timeout=1, max=1000")

        val isSessionPresent = request.cookies?.filter { it.name == "x-session-id" }

        if(isSessionPresent == null) {
            val session = identificationService.getSSAISession(request)
            response.addCookie(Cookie("x-session-id", session.id).apply {
                this.path = "/"
                this.maxAge = 172800
            })

            response.addCookie(Cookie("x-session-id", session.id).apply {
                this.path = "/media"
                this.maxAge = 172800
            })

            _session.id = session.id
        } else {
            val session = identificationService.getSSAISession(request)
            _session.id = session.id
        }

        if (!request.method.equals("OPTIONS", ignoreCase = true)) {
            try {
                val timeInMillis = MeasureTools.measureTimeInMillis {
                    chain!!.doFilter(req, res)
                }
            } catch (ex: Exception) {
                if(ex !is ClientAbortException) {
                   log.error("Global error", ex)
                }
            }
        } else {
            println("Pre-flight")
            response.setHeader("Access-Control-Allowed-Methods", "POST, GET, DELETE")
            response.setHeader("Access-Control-Max-Age", "3600")
            response.setHeader("Access-Control-Allow-Headers", "authorization, content-type,x-auth-token, " +
                    "access-control-request-headers, access-control-request-method, accept, origin, authorization, x-requested-with")
            response.status = HttpServletResponse.SC_OK
        }
    }

}