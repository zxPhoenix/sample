package com.postindustria.ssai.streaming.messaging

import com.postindustria.ssai.common.model.Stream
import java.util.concurrent.Semaphore

class LoadStreamTask(stream: Stream){
    private val threads = mutableListOf<Any>()
    var stream: Stream? = null
    var lock: Semaphore? = null
    var hasError: Boolean = false
}