package com.postindustria.ssai.common.model.sessions

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed

@RedisHash("streams_state")
data class Stream(
    @Id var id: String?,
    @Indexed var targetUrl: String?,
    val streams: List<Session> = mutableListOf()
)