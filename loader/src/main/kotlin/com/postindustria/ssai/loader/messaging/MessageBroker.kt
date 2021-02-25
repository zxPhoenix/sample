package com.postindustria.ssai.loader.messaging

import com.postindustria.ssai.common.model.*
import com.postindustria.ssai.loader.Engine
import org.springframework.beans.factory.annotation.Autowired

open abstract class MessageBroker {
    @Autowired
    lateinit var engine: Engine

    open abstract fun notifyWorkerStateChanged(loaderMetadata: LoaderMetadata)

    fun onNewLoaderCallback(event: PlayStreamEvent){
        println("new loader task:$event")
        engine.load(event)
    }

    fun errorFunction(stream: Stream) {

    }
}