package com.postindustria.ssai.streaming.messaging

import com.postindustria.ssai.common.model.LoaderMetadata
import com.postindustria.ssai.common.model.PlayStreamEvent
import com.postindustria.ssai.common.model.Stream
import com.postindustria.ssai.common.model.WorkerMetadata

interface IMessageBroker {
    fun runSplitter(event: PlayStreamEvent)
    fun runLoader(event: PlayStreamEvent)
    fun setSplitterOnResultCallback(function: (workerMetadata: WorkerMetadata)-> Unit)
    fun setLoaderOnResultCallback(function: (workerMetadata: LoaderMetadata)-> Unit)
    fun setWorkerOnErrorCallback(function: (stream: Stream)-> Unit)
    fun sendNewSplitterEvent(event: PlayStreamEvent)
    fun sendNewLoaderEvent(event: PlayStreamEvent)
    fun sendStreamIsAliveMessage(target_url: String)
}