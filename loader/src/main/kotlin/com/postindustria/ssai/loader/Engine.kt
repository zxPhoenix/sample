package com.postindustria.ssai.loader

import com.postindustria.ssai.common.model.LoaderMetadata
import com.postindustria.ssai.common.model.PlayStreamEvent
import com.postindustria.ssai.common.model.log
import com.postindustria.ssai.loader.messaging.gcp.GCPMessageBroker
import com.postindustria.ssai.loader.repository.Repository
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.*

class Engine {
    private var loadersProvider: ObjectProvider<Loader>? = null
    private var autoBlockingDeque = LinkedBlockingQueue<Runnable>()
    var executorService = ThreadPoolExecutor(2, 16, 30, TimeUnit.SECONDS, autoBlockingDeque)
    var loaderMap = TreeMap<String, Loader>()

    @Autowired
    lateinit var repository: Repository
    @Autowired
    lateinit var messageBroker: GCPMessageBroker

    @Autowired
    fun ProcessorsPrototype(loaderProvider: ObjectProvider<Loader>?) {
        loaderProvider?.let {
            this.loadersProvider = it
        }
    }

    fun load(event: PlayStreamEvent){
        process(event, "yyy.m3u8")
    }

    @Synchronized
    fun process(event: PlayStreamEvent, filename: String) {
        val src = event.stream?.source_url!!
        if(loaderMap.get(src) == null) {
            val loader = loadersProvider?.getObject()
            loaderMap.put(src, loader as Loader)

            executorService.execute {
                try {
                    loader?.load(event, filename)
                } catch (e: Exception) {
                    log.error("global exception[$src]:", e)
                }
            }
        } else {
            notifyStateChanged(event, LoaderMetadata.STATE.LIVE)
        }
    }

    fun notifyStateChanged(event: PlayStreamEvent, status: LoaderMetadata.STATE, msg: String = "") {
        val key = getLoaderKey(event)

        log.info("notifyStateChanged: Stream state changed - {$key} : {$status}")

        var metadata: Optional<LoaderMetadata>? = repository.getLoaderState(key)
        if (metadata != null && metadata.isPresent) {
            log.info("notifyStateChanged: Stream state changed - {$key} : {$status}!!!!")
            var sm: LoaderMetadata = metadata.get()
            sm.apply {
                state = status
                target_url = event.stream?.target_url.toString()
                timestamp = System.currentTimeMillis()
                message = msg
                request_id = getKey(event)
            }
            repository.updateLoaderState(sm)

            messageBroker.notifyWorkerStateChanged(sm)
        }
        else {
            log.warn("StreamManager.notifyStateChanged: Stream metadata not found.");
        }
    }

    fun destroy(){
        loaderMap.forEach { key, loader ->
            loader.destroy()
        }
    }

    private fun getLoaderKey(event: PlayStreamEvent) = "Loader:" + event.stream?.target_url
    fun getKey(event: PlayStreamEvent) = event.stream?.target_url.toString() + "::::" + event.session?.id
}