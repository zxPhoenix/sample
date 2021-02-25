package com.postindustria.ssai.front.messaging

import com.postindustria.ssai.common.model.Stream
import java.util.concurrent.Semaphore

class RunWorkerTask(stream: Stream){
    var stream: Stream? = null
    var lock: Semaphore? = null
    var hasError: Boolean = false
}