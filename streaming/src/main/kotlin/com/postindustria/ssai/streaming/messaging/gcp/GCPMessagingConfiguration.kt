package com.postindustria.ssai.streaming.messaging.gcp

import com.fasterxml.jackson.databind.ObjectMapper
import com.postindustria.ssai.common.model.LoaderMetadata
import com.postindustria.ssai.common.model.PlayStreamEvent
import com.postindustria.ssai.common.model.StreamIsAliveEvent
import com.postindustria.ssai.common.model.WorkerMetadata
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.gcp.pubsub.PubSubAdmin
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate
import org.springframework.cloud.gcp.pubsub.integration.AckMode
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import java.net.InetAddress
import javax.annotation.PreDestroy


@Configuration
@EnableIntegration
class GCPMessagingConfiguration : WebMvcConfigurerAdapter() {
    @Autowired
    lateinit var  pubSubAdmin: PubSubAdmin

    @Bean
    @ServiceActivator(inputChannel = "ssaiChannel")
    fun messageSender(pubsubTemplate: PubSubTemplate?): MessageHandler? {
        return PubSubMessageHandler(pubsubTemplate, "topic_worker_run")
    }

    @Bean
    @ServiceActivator(inputChannel = "ssaiLoaderChannel")
    fun messageNewChannelSender(pubsubTemplate: PubSubTemplate?): MessageHandler? {
        return PubSubMessageHandler(pubsubTemplate, "topic_loader_run")
    }

    @Bean
    @ServiceActivator(inputChannel = "channel_stream_is_alive")
    fun messageStreamIsAliveSender(pubsubTemplate: PubSubTemplate?): MessageHandler? {
        return PubSubMessageHandler(pubsubTemplate, "topic_stream_is_alive")
    }

    @MessagingGateway(defaultRequestChannel = "ssaiChannel")
    interface PubsubWorkerRunGateway {
        fun sendEvent(text: PlayStreamEvent?)
    }

    @MessagingGateway(defaultRequestChannel = "ssaiLoaderChannel")
    interface PubsubLoaderRunGateway {
        fun sendEvent(text: PlayStreamEvent?)
    }

    @MessagingGateway(defaultRequestChannel = "channel_stream_is_alive")
    interface PubsubStreamIsAliveGateway {
        fun sendStreamIsAliveEvent(event: StreamIsAliveEvent?)
    }

    @Bean(name = ["ssaiChannel"])
    fun channelWorkerRun(): MessageChannel? {
        return DirectChannel()
    }

    @Bean(name = ["ssaiLoaderChannel"])
    fun channelLoaderRun(): MessageChannel? {
        return DirectChannel()
    }

    @Bean(name = ["channel_stream_is_alive"])
    fun channelStreamIsAlive(): MessageChannel? {
        return DirectChannel()
    }

    @Bean(name = ["channel_worker_status_changed"])
    fun channelWorkerStatusChanged(): MessageChannel? {
        return DirectChannel()
    }

    @Bean(name = ["channel_loader_status_changed"])
    fun channelLoaderStatusChanged(): MessageChannel? {
        return DirectChannel()
    }

    @Bean
    fun messageWorkerStatusChangedChannelAdapter(@Qualifier("channel_worker_status_changed") inputChannel: MessageChannel?,
                                                 pubSubTemplate: PubSubTemplate?): PubSubInboundChannelAdapter? {
        init()
        val adapter = PubSubInboundChannelAdapter(pubSubTemplate, "subscription_worker_status_changed_" + getIp())
        adapter.outputChannel = inputChannel
        adapter.ackMode = AckMode.MANUAL
        adapter.payloadType = WorkerMetadata::class.java
        return adapter
    }

    @Bean
    fun messageLoaderStatusChangedChannelAdapter(@Qualifier("channel_loader_status_changed") inputChannel: MessageChannel?,
                                                 pubSubTemplate: PubSubTemplate?): PubSubInboundChannelAdapter? {
        init()
        val adapter = PubSubInboundChannelAdapter(pubSubTemplate, "subscription_loader_status_changed_" + getIp())
        adapter.outputChannel = inputChannel
        adapter.ackMode = AckMode.MANUAL
        adapter.payloadType = LoaderMetadata::class.java
        return adapter
    }

    @Bean
    fun messageChannelAdapter(@Qualifier("ssaiChannel") inputChannel: MessageChannel?,
            pubSubTemplate: PubSubTemplate?): PubSubInboundChannelAdapter? {
        init()
        val adapter = PubSubInboundChannelAdapter(pubSubTemplate, "subscription_worker_run")
        adapter.outputChannel = inputChannel
        adapter.ackMode = AckMode.AUTO_ACK
        adapter.payloadType = PlayStreamEvent::class.java
        return adapter
    }

    /*@Bean
    fun messageStreamIsAliveChannelAdapter(@Qualifier("channel_stream_is_alive") inputChannel: MessageChannel?,
                              pubSubTemplate: PubSubTemplate?): PubSubInboundChannelAdapter? {
        init()
        val adapter = PubSubInboundChannelAdapter(pubSubTemplate, "subscription_stream_is_alive")
        adapter.outputChannel = inputChannel
        adapter.ackMode = AckMode.AUTO_ACK
        adapter.payloadType = StreamIsAliveEvent::class.java
        return adapter
    }*/

    @Bean
    fun jacksonPubSubMessageConverter(): JacksonPubSubMessageConverter? {
        return JacksonPubSubMessageConverter(ObjectMapper())
    }

    fun init(){
        var topicWorkerRun = pubSubAdmin.getTopic("topic_worker_run") ?: pubSubAdmin.createTopic("topic_worker_run")
        var topicWorkerStatusChanged = pubSubAdmin.getTopic("topic_worker_status_changed") ?: pubSubAdmin.createTopic("topic_worker_status_changed")
        var topicStreamIsAlive = pubSubAdmin.getTopic("topic_stream_is_alive") ?: pubSubAdmin.createTopic("topic_stream_is_alive")

        var topicLoaderRun = pubSubAdmin.getTopic("topic_loader_run") ?: pubSubAdmin.createTopic("topic_loader_run")
        var topicLoaderStatusChanged = pubSubAdmin.getTopic("topic_loader_status_changed") ?: pubSubAdmin.createTopic("topic_loader_status_changed")


        var subscriptionWorkerRun = pubSubAdmin.getSubscription("subscription_worker_run") ?: pubSubAdmin.createSubscription("subscription_worker_run", topicWorkerRun.name, 20)
        var subscriptionWorkerStatusChanged = pubSubAdmin.getSubscription("subscription_worker_status_changed_" + getIp()) ?: pubSubAdmin.createSubscription("subscription_worker_status_changed_" + getIp(), topicWorkerStatusChanged.name, 20)
        var subscriptionLoaderStatusChanged = pubSubAdmin.getSubscription("subscription_loader_status_changed_" + getIp()) ?: pubSubAdmin.createSubscription("subscription_loader_status_changed_" + getIp(), topicLoaderStatusChanged.name, 20)
        var subscriptionStreamIsAlive = pubSubAdmin.getSubscription("subscription_stream_is_alive") ?: pubSubAdmin.createSubscription("subscription_stream_is_alive", topicStreamIsAlive.name, 20)
    }

    fun getIp(): String {
        val address = InetAddress.getLocalHost()
        return address.hostAddress
    }

    @PreDestroy
    fun onDestroy(){
        pubSubAdmin.getSubscription("subscription_worker_status_changed_" + getIp())?.let {
            pubSubAdmin.deleteSubscription("subscription_worker_status_changed_" + getIp())
        }
    }
}