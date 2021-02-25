package com.postindustria.ssai.streaming.usecases

import com.postindustria.ssai.common.model.SSAISession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import javax.servlet.http.HttpServletRequest

@Component
class IdentificationService {
    @Autowired
    lateinit var session: SSAISession

    fun getSSAISession(request: HttpServletRequest): SSAISession {
        return session.apply {
            id = getSessionId(request) ?.let { it } ?: UUID.randomUUID().toString()
        }
    }

    private fun getSessionId(request: HttpServletRequest): String?{
        return request.cookies?.filter { it.name == "x-session-id" }?.let { if(it.isNotEmpty()) it[0]?.value else null } ?: null
    }
}