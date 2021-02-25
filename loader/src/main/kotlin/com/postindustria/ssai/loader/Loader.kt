package com.postindustria.ssai.loader

import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult
import com.github.kokorin.jaffree.ffmpeg.UrlInput
import com.github.kokorin.jaffree.ffmpeg.UrlOutput
import com.github.kokorin.jaffree.ffprobe.FFprobe
import com.postindustria.ssai.common.model.LoaderMetadata
import com.postindustria.ssai.common.model.PlayStreamEvent
import com.postindustria.ssai.common.model.WorkerMetadata
import com.postindustria.ssai.common.model.log
import com.postindustria.ssai.common.repository.GC
import com.postindustria.ssai.loader.common.VideoTools
import com.postindustria.ssai.loader.repository.Repository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.MessageDeliveryException
import java.net.URLDecoder
import java.nio.file.Paths
import java.util.concurrent.Future

class Loader() {
    private var ffmpeg: FFmpeg? = null
    private var task: Future<FFmpegResult>? = null
    private lateinit var event: PlayStreamEvent

    @Autowired
    lateinit var engine: Engine
    @Autowired
    lateinit var repository: Repository
    var isSSAINotified = false

    var streamPath: String? = null

    fun load(event: PlayStreamEvent, filename: String) {
        this.event = event
        val url = URLDecoder.decode(event.stream?.source_url.toString(), "UTF-8")

        repository.db.findBySource(url)?.let {
            streamPath = repository.createStremFolderIfNotExists(it.target_url.toString())
        }

        if(streamPath == null) {
            return
        }

        ffmpeg = FFmpeg(Paths.get("/Users/mac/Downloads/ff/bin/ffmpeg"))

        ffmpeg?.addInput(UrlInput().setInput(url))

         FFprobe(Paths.get( "/Users/mac/Downloads/ff/bin/ffprobe"))
                .setInput(url)
                .setShowStreams(true)
                .setShowPrograms(true)
                .execute()?.let {
                     log.error("_____________start_time: " + it.streams[0].startPts)
                     for (stream in it.streams) {
                         log.error("__start: " + stream.startTime)
                         log.error("__startPtsЗ: " + stream.startPts)
                     }

                     it.programs?.forEachIndexed { index, program ->
                         var bitrates = VideoTools.calcBitrates(program)
                         if(bitrates.video > -1) {
                             ffmpeg?.let {
                                 it.addArguments("-map", "0:p:$index:v:0")
                                    .addArguments("-map", "0:p:$index:a:0")

                                    .addArguments("-b:v:$index", "" + bitrates.video + "K")
                                    .addArguments("-minrate:$index", "" + bitrates.video + "K")
                                    .addArguments("-maxrate:$index", "" + bitrates.video + "K")
                             }
                         }
                     }

                     var str =""

                     it.programs?.forEachIndexed { index, program ->
                         var bitrates = VideoTools.calcBitrates(program)
                         if(bitrates.video > -1){
                             str += "v:$index,a:$index "
                         }
                     }

                     ffmpeg?.addArguments("-c:v", "copy")
                     ffmpeg?.setProgressListener { progress ->
                         if(!isSSAINotified) {
                             isSSAINotified = true
                             engine.notifyStateChanged(event, LoaderMetadata.STATE.LIVE)
                         }
                         log("performance").info("progress speed: " + progress.speed + ", fps: " + progress.fps + ", url: " + url)
                     }
                     ffmpeg?.addOutput(UrlOutput()
                             .addArguments("-f", "hls")
                             .addArguments("-sc_threshold", "0")
                             .addArguments("-hls_time", "10")
                             .addArguments("-hls_list_size", "10")
                             .addArguments("-hls_delete_threshold","10")
                             .addArguments("-hls_flags", "delete_segments+omit_endlist")
                             .addArguments("-hls_segment_filename", repository.store + "/src/" + streamPath + "/stream_%v/file%d.ts")
                             .addArguments("-var_stream_map", str)
                             .addArguments("-master_pl_name", "master.m3u8")
                             .addArguments("-master_pl_publish_rate", "1")
                             .setOutput(repository.store + "/src/" + streamPath + "/stream_%v/pl.m3u8")
                     )
                     task = ffmpeg?.executeAsync()
        }
    }

    fun destroy() {
        task?.cancel(true)
        try{
            event?.let { a -> engine.notifyStateChanged(a, LoaderMetadata.STATE.SUSPEND) }
        } catch (e: MessageDeliveryException) { }

        val path = repository.store + "/src/" + streamPath

        log.info("Clear storage: " + path)
        GC.clearStorage(path)
    }
}