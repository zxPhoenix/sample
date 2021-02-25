package com.postindustria.ssai

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FrontApplication

val logger: Logger by lazy {
    LoggerFactory.getLogger("main")
}

fun main(args: Array<String>) {
    runApplication<FrontApplication>(*args)
}

fun Any.log(msg: String) {
    logger.info(msg)
}

fun Any.logError(msg: String) {
    logger.error(msg)
}

fun Any.logError(msg: String, t: Throwable) {
    logger.error(msg, t)
}

fun Any.log(tag: String, msg: String) {
    LoggerFactory.getLogger(tag).info(msg)
}

fun Any.logError(tag: String, msg: String) {
    LoggerFactory.getLogger(tag).error(msg)
}

fun Any.logError(tag: String, msg: String, t: Throwable) {
    LoggerFactory.getLogger(tag).error(msg, t)
}
