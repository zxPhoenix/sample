package com.postindustria.ssai.streaming.messaging.gcp

import com.postindustria.ssai.common.model.*
import com.postindustria.ssai.streaming.messaging.IMessageBroker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders
import org.springframework.context.annotation.Bean
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandler
import org.springframework.stereotype.Component


@Component
class GCPMessageBroker: IMessageBroker {
    private lateinit var splitterOnResultCallback: (workerMetadata: WorkerMetadata) -> Unit
    private lateinit var loaderOnResultCallback: (workerMetadata: LoaderMetadata) -> Unit

    @Autowired
    private lateinit var pubsubSplitterRunGateway: GCPMessagingConfiguration.PubsubWorkerRunGateway

    @Autowired
    private lateinit var pubsubLoaderRunGateway: GCPMessagingConfiguration.PubsubLoaderRunGateway

    @Autowired
    private lateinit var pubsubStreamIsAliveGateway: GCPMessagingConfiguration.PubsubStreamIsAliveGateway
    override fun runSplitter(event: PlayStreamEvent) {
        pubsubSplitterRunGateway.sendEvent(event)
    }

    override fun runLoader(event: PlayStreamEvent) {
        pubsubLoaderRunGateway.sendEvent(event)
    }

    override fun setSplitterOnResultCallback(function: (workerMetadata: WorkerMetadata) -> Unit) {
        splitterOnResultCallback = function
    }

    override fun setLoaderOnResultCallback(function: (workerMetadata: LoaderMetadata) -> Unit) {
        loaderOnResultCallback = function
    }

    override fun setWorkerOnErrorCallback(function: (stream: Stream) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendNewSplitterEvent(event: PlayStreamEvent) {
        pubsubSplitterRunGateway.sendEvent(event)
    }

    override fun sendNewLoaderEvent(event: PlayStreamEvent) {
        pubsubLoaderRunGateway.sendEvent(event)
    }

    override fun sendStreamIsAliveMessage(target_url: String) {
        pubsubStreamIsAliveGateway.sendStreamIsAliveEvent(StreamIsAliveEvent(target_url))
    }

    @Bean
    @ServiceActivator(inputChannel = "channel_worker_status_changed")
    fun messageReceiver(): MessageHandler? {
        return MessageHandler { message: Message<*> ->
            splitterOnResultCallback.invoke(message.payload as WorkerMetadata)
            val originalMessage = message.headers.get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage::class.java)
            originalMessage?.ack()
        }
    }

    @Bean
    @ServiceActivator(inputChannel = "channel_loader_status_changed")
    fun messageReceiver1(): MessageHandler? {
        return MessageHandler { message: Message<*> ->
            loaderOnResultCallback.invoke(message.payload as LoaderMetadata)
            val originalMessage = message.headers.get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage::class.java)
            originalMessage?.ack()
        }
    }
}