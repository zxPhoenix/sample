package com.postindustria.ssai.loader.messaging.gcp

import com.postindustria.ssai.common.model.LoaderMetadata
import com.postindustria.ssai.common.model.PlayStreamEvent
import com.postindustria.ssai.loader.messaging.MessageBroker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders
import org.springframework.context.annotation.Bean
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandler
import org.springframework.stereotype.Component

@Component
class GCPMessageBroker: MessageBroker() {
    @Autowired
    private lateinit var pubsubGateway: GCPMessagingConfiguration.PubsubGateway

    override fun notifyWorkerStateChanged(loaderMetadata: LoaderMetadata) {
        pubsubGateway.notifyWorkerStatusChanged(loaderMetadata)
    }

    @Bean
    @ServiceActivator(inputChannel = "ssaiChannel")
    fun messageReceiver(): MessageHandler? {
        return MessageHandler { message: Message<*> ->
            val originalMessage = message.headers.get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage::class.java)
            originalMessage?.ack()

            onNewLoaderCallback(message.payload as PlayStreamEvent)
        }
    }
}