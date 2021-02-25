package com.postindustria.ssai.front.mvc

import com.postindustria.ssai.front.usecases.IdentificationService
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

    override fun doFilter(req: ServletRequest?, res: ServletResponse?, chain: FilterChain?) {
        val request = req as HttpServletRequest
        val response = res as HttpServletResponse

        response.setHeader("Access-control-Allow-Origin", "*")
        response.setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE")
        response.setHeader("Access-Control-Allow-Headers", "*")
        response.setHeader("Access-Control-Max-Age", "3600")
        response.setHeader("Access-Control-Allow-Credentials", "true")

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
        }

        if (!request.method.equals("OPTIONS", ignoreCase = true)) {
            try {
                chain!!.doFilter(req, res)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } else {
            response.setHeader("Access-Control-Allowed-Methods", "POST, GET, DELETE")
            response.setHeader("Access-Control-Max-Age", "3600")
            response.setHeader("Access-Control-Allow-Headers", "authorization, content-type,x-auth-token, " +
                    "access-control-request-headers, access-control-request-method, accept, origin, authorization, x-requested-with")
            response.status = HttpServletResponse.SC_OK
        }
    }

}