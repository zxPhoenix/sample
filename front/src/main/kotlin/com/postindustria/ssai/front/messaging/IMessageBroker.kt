package com.postindustria.ssai.front.messaging

import com.postindustria.ssai.common.model.PlayStreamEvent
import com.postindustria.ssai.common.model.Stream
import com.postindustria.ssai.common.model.WorkerMetadata

interface IMessageBroker {
    fun runWorker(event: PlayStreamEvent)
    fun setWorkerOnResultCallback(function: (workerMetadata: WorkerMetadata)-> Unit)
    fun setWorkerOnErrorCallback(function: (stream: Stream)-> Unit)
    fun sendNewWorkerMessage(event: PlayStreamEvent)
    fun sendStreamIsAliveMessage(target_url: String)
}