package com.postindustria.ssai.common.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.redis.core.RedisHash
import javax.persistence.Id

@RedisHash("LoaderMetadata")
data class LoaderMetadata(
        @JsonProperty("id")
        var id: String? = "",
        @JsonProperty("target_url")
        var target_url: String? = "",
        @get:Id
        @JsonProperty("state")
        var state: STATE = STATE.NEW,
        @JsonProperty("timestamp")
        var timestamp: Long = System.currentTimeMillis(),
        @JsonProperty("message")
        var message: String = "",
        @JsonProperty("request_id")
        var request_id: String = ""
){
    enum class STATE { NEW, LIVE, SUSPEND, ERROR }
}