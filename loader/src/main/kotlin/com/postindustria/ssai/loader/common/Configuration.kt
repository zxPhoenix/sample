package com.postindustria.ssai.loader.common

import com.postindustria.ssai.loader.Engine
import com.postindustria.ssai.loader.Loader
import com.postindustria.ssai.loader.loader.M3U8Loader
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.*
import org.springframework.context.annotation.Configuration
import org.springframework.integration.config.EnableIntegration
import org.springframework.scheduling.annotation.EnableScheduling


@Configuration
@EnableScheduling
@EnableIntegration
@ComponentScan("com.postindustria.ssai")
@EntityScan("com.postindustria.ssai.*")
@PropertySources(
        PropertySource("classpath:application.properties"),
        PropertySource("classpath:application-local.properties")
)
class Configuration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun getM3U8Loader(): M3U8Loader = M3U8Loader()

    @Bean(destroyMethod = "destroy")
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun getEngine(): Engine = Engine()

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun getProcessor(): Loader = Loader()
}