package com.postindustria.ssai.loader

import com.postindustria.ssai.common.model.PlayStreamEvent
import com.postindustria.ssai.common.model.Stream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.*
import javax.annotation.PostConstruct

@SpringBootApplication
class LoaderApplication {

	@Autowired
	lateinit var engine: Engine

	@PostConstruct
	fun doit() {
		//Predefined streams for testing purposes
		//engine.newTask("https://content.jwplatform.com/manifests/yp34SRmf.m3u8")
		//engine.newTask("http://cdn1.live-tv.od.ua:8081/a1od/a1od-abr/playlist.m3u8")
		//engine.load("http://cdn-videos.akamaized.net/btv/desktop/akamai/europe/live/primary.m3u8")
		//engine.load("https://abclive1-lh.akamaihd.net/i/abc_live04@423398/master.m3u8")
		/*engine.load( PlayStreamEvent().apply{
			this.stream = Stream("http://svc-lvanvato-cxtv-wpxi.cmgvideo.com/wpxi/master.m3u8").apply {
				this.target_url = UUID.fromString("4aa3191a-8955-4a86-82f5-01920616ac8b")
			}

		})*/
		//engine.newTask("http://127.0.0.1:8088/m3u8/master.m3u8")
	}
}

fun main(args: Array<String>) {
	runApplication<LoaderApplication>(*args)
}
