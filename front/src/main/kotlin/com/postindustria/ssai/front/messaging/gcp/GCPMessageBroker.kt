package com.postindustria.ssai.front.messaging.gcp

import com.postindustria.ssai.common.model.PlayStreamEvent
import com.postindustria.ssai.front.messaging.IMessageBroker
import com.postindustria.ssai.common.model.Stream
import com.postindustria.ssai.common.model.StreamIsAliveEvent
import com.postindustria.ssai.common.model.WorkerMetadata
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
    private lateinit var workerOnResultCallback: (workerMetadata: WorkerMetadata) -> Unit
    @Autowired
    private lateinit var pubsubWorkerRunGateway: GCPMessagingConfiguration.PubsubWorkerRunGateway
    @Autowired
    private lateinit var pubsubStreamIsAliveGateway: GCPMessagingConfiguration.PubsubStreamIsAliveGateway
    override fun runWorker(event: PlayStreamEvent) {
        pubsubWorkerRunGateway.sendToPubsub(event)
    }

    override fun setWorkerOnResultCallback(function: (workerMetadata: WorkerMetadata) -> Unit) {
        workerOnResultCallback = function
    }

    override fun setWorkerOnErrorCallback(function: (stream: Stream) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendNewWorkerMessage(event: PlayStreamEvent) {
        pubsubWorkerRunGateway.sendToPubsub(event)
    }

    override fun sendStreamIsAliveMessage(target_url: String) {
        pubsubStreamIsAliveGateway.sendStreamIsAliveEvent(StreamIsAliveEvent(target_url))
    }

    @Bean
    @ServiceActivator(inputChannel = "channel_worker_status_changed")
    fun messageReceiver(): MessageHandler? {
        return MessageHandler { message: Message<*> ->
            workerOnResultCallback.invoke(message.payload as WorkerMetadata)
            val originalMessage = message.headers.get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage::class.java)
            originalMessage?.ack()
        }
    }
}