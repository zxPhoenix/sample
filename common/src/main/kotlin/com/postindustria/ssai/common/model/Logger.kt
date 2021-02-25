package com.postindustria.ssai.common.model

import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger by lazy {
    LoggerFactory.getLogger("main")
}

val Any.log: Logger
    get() = logger

fun Any.log(tag: String): Logger =
        LoggerFactory.getLogger(tag)

fun Any.measureTimeInMillis(func: ()->Unit): Long {
    val time = System.currentTimeMillis()

    func.invoke()

    return System.currentTimeMillis() - time
}