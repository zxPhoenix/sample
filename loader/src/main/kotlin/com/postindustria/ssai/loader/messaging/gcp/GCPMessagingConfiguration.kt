package com.postindustria.ssai.loader.messaging.gcp

import com.fasterxml.jackson.databind.ObjectMapper
import com.postindustria.ssai.common.model.LoaderMetadata
import com.postindustria.ssai.common.model.PlayStreamEvent
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
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import java.net.InetAddress
import javax.annotation.PreDestroy

@Configuration
@EnableIntegration
class GCPMessagingConfiguration : WebMvcConfigurerAdapter() {
    @Autowired
    lateinit var  pubSubAdmin: PubSubAdmin
    @Autowired
    lateinit var messageBroker: GCPMessageBroker

    @Bean
    @ServiceActivator(inputChannel = "channel_loader_status_changed")
    fun messageSender(pubsubTemplate: PubSubTemplate?): PubSubMessageHandler {
        return PubSubMessageHandler(pubsubTemplate, "topic_loader_status_changed")
    }

    @MessagingGateway(defaultRequestChannel = "channel_loader_status_changed")
    interface PubsubGateway {
        fun notifyWorkerStatusChanged(status: LoaderMetadata?)
    }

    @Bean(name = ["channel_loader_status_changed"])
    fun channelWorkerStatusChanged(): MessageChannel? {
        return DirectChannel()
    }

    @Bean(name = ["ssaiChannel"])
    fun appTypeUpgradeInput(): MessageChannel? {
        return DirectChannel()
    }

    @Bean
    fun messageWorkerRunChannelAdapter(@Qualifier("ssaiChannel") inputChannel: MessageChannel?,
            pubSubTemplate: PubSubTemplate?): PubSubInboundChannelAdapter? {
        init()
        val adapter = PubSubInboundChannelAdapter(pubSubTemplate, "subscription_loader_run")
        adapter.outputChannel = inputChannel
        adapter.ackMode = AckMode.MANUAL
        adapter.payloadType = PlayStreamEvent::class.java
        return adapter
    }

    @Bean
    fun jacksonPubSubMessageConverter(): JacksonPubSubMessageConverter? {
        return JacksonPubSubMessageConverter(ObjectMapper())
    }

    fun init(){
        var topicLoaderRun = pubSubAdmin.getTopic("topic_loader_run") ?: pubSubAdmin.createTopic("topic_loader_run");
        var topicLoaderStatusChanged = pubSubAdmin.getTopic("topic_loader_status_changed") ?: pubSubAdmin.createTopic("topic_loader_status_changed");
        var subscriptionWorkerRun = pubSubAdmin.getSubscription("subscription_loader_run") ?: pubSubAdmin.createSubscription("subscription_loader_run", topicLoaderRun.name, 20)
        var subscriptionWorkerStatusChanged = pubSubAdmin.getSubscription("subscription_worker_loader_changed") ?: pubSubAdmin.createSubscription("subscription_worker_loader_changed", topicLoaderStatusChanged.name, 20)
    }

    fun getIp(): String {
        val address = InetAddress.getLocalHost()
        return address.hostAddress
    }

    @PreDestroy
    fun onDestroy(){
    }
}