package com.postindustria.ssai.streaming.mvc

import com.postindustria.ssai.common.model.SSAISession
import com.postindustria.ssai.common.monitoring.gcp.GCPMonitoring
import com.postindustria.ssai.streaming.messaging.IMessageBroker
import com.postindustria.ssai.streaming.messaging.gcp.GCPMessageBroker
import com.postindustria.ssai.streaming.repository.Repository
import com.postindustria.ssai.streaming.repository.StorageDataSource
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.stackdriver.StackdriverConfig
import io.micrometer.stackdriver.StackdriverMeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.actuate.autoconfigure.metrics.web.tomcat.TomcatMetricsAutoConfiguration
import org.springframework.context.annotation.*
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.redis.util.RedisLockRegistry
import org.springframework.integration.support.locks.LockRegistry
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextListener
import org.springframework.web.servlet.config.annotation.*
import java.time.Duration
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

    @Autowired
    lateinit var interceptor: Interceptor

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

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(interceptor)
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

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun getMVCInterceptor(): Interceptor {
        return Interceptor()
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun getLockRegistry(connectionFactory: LettuceConnectionFactory): LockRegistry {
        return RedisLockRegistry(connectionFactory, "services", 60000)
    }

    @Bean
    fun threadMetrics(): JvmThreadMetrics? {
        return JvmThreadMetrics()
    }

    @Bean
    fun tomcatMetrics(): TomcatMetricsAutoConfiguration? {
        return TomcatMetricsAutoConfiguration()
    }

    @Bean
    fun stackdriver(): StackdriverMeterRegistry? {
        return StackdriverMeterRegistry.builder(object : StackdriverConfig {
            override fun projectId(): String {
                return "ssai-273011"
            }

            override fun get(key: String): String? {
                return null
            }

        }).build()
    }
}

@WebListener
class MyRequestContextListener : RequestContextListener()