package com.postindustria.ssai.common.model.sessions

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed

@RedisHash("streams_sessions")
data class Session(
    @Id var id: String?,
    @Indexed var streamId: String,
    var timestamp: Long
)