package com.postindustria.ssai.front.mvc

import com.postindustria.ssai.common.model.SSAISession
import com.postindustria.ssai.common.monitoring.gcp.GCPMonitoring
import com.postindustria.ssai.front.messaging.IMessageBroker
import com.postindustria.ssai.front.messaging.gcp.GCPMessageBroker
import com.postindustria.ssai.front.repository.Repository
import com.postindustria.ssai.front.repository.StorageDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.*
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericToStringSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.integration.config.EnableIntegration
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextListener
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import javax.servlet.annotation.WebListener


@Configuration
@EnableIntegration
@ComponentScan("com.postindustria.ssai")
@PropertySources(
    PropertySource("classpath:application.properties"),
    PropertySource("classpath:application-local.properties")
)
class Configuration : WebMvcConfigurerAdapter() {
    companion object {
        private val CLASSPATH_RESOURCE_LOCATIONS = arrayOf(
                "classpath:/META-INF/resources/", "classpath:/resources/",
                "classpath:/static/", "classpath:/public/")
    }

    @Autowired
    lateinit var lettuceConnectionFactory: LettuceConnectionFactory

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(*CLASSPATH_RESOURCE_LOCATIONS)
    }

    @Bean
    fun corsConfigurer(): WebMvcConfigurer? {
        return object : WebMvcConfigurerAdapter() {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH");
            }
        }
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun getRepository(): Repository? {
        return Repository()
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun getMessageBroker(messageBroker: GCPMessageBroker?): IMessageBroker? {
        return messageBroker
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun getStorageDataSource(): StorageDataSource {
        return StorageDataSource()
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun getGCPMonitoring(): GCPMonitoring {
        return GCPMonitoring()
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    fun getSSAISession(): SSAISession {
        return SSAISession()
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any>? {
        val template = RedisTemplate<String, Any>()
        template.setConnectionFactory(lettuceConnectionFactory)
        template.valueSerializer = StringRedisSerializer()
        return template
    }
}

@WebListener
class MyRequestContextListener : RequestContextListener()