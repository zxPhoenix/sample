package com.postindustria.dai.front.model

class AdsAPIResponse {
    var duration: Long = 0
    val ads: MutableList<Ad> = mutableListOf()
    var blank_stream: Ad? = null
}