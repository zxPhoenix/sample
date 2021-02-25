package com.postindustria.ssai.streaming.mvc

import com.postindustria.ssai.common.model.SSAISession
import com.postindustria.ssai.common.model.log
import com.postindustria.ssai.common.monitoring.MeasureTools
import com.postindustria.ssai.common.monitoring.Monitoring
import com.postindustria.ssai.streaming.usecases.Services
import io.micrometer.core.annotation.Timed
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.catalina.connector.ClientAbortException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.web.servlet.view.RedirectView
import java.io.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
@Timed(value = "front")
class RESTController {
    @Autowired
    lateinit var services: Services

    @Autowired
    lateinit var monitoring: Monitoring

    @Autowired
    lateinit var session: SSAISession

    @Value("\${worker.store}")
    lateinit var storePath: String

    @GetMapping("/play/{id}.m3u8")
    @Timed(value = "front.init_stream", longTask = true)
    fun play(request: HttpServletRequest, @PathVariable("id") id: String): RedirectView? {
        var res: RedirectView? = null
        var sessionId = session.id

        if(sessionId == null) {
            sessionId = "xxx"
        }

        res = services.getStream(id, sessionId)?.let{
            RedirectView("/media/${it.target_url}/" + sessionId +"/master.m3u8" ?: "/")
        } ?: RedirectView("media/no_result")

        return res
    }

    @GetMapping(value = ["/media/{path}/{user}/{file}.m3u8"], produces = [MediaType.TEXT_PLAIN_VALUE])
    @Timed(value = "front.playlist", longTask = true)
    @ResponseBody
    @Throws(IOException::class)
    fun getPlaylist(@PathVariable("path") path: String, @PathVariable("user") user: String, @PathVariable("file") file: String): ByteArray? {
        var res: ByteArray? = null;
        try {
            res = services.getFileStream(user, path + "/" + user, file + ".m3u8")
        } catch (e: Exception) {
            try {
                Thread.sleep(500)
                res = services.getFileStream(user, path + "/" + user, file + ".m3u8")
            } catch (e1: Exception) {

            }
        }

        return res
    }

    @GetMapping(value = ["/media/{path}/{stream}/{file}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    @Timed(value = "front.chunk", longTask = true)
    fun getSharedTS(response: HttpServletResponse, @PathVariable("path") path: String, @PathVariable("stream") stream: String, @PathVariable("file") file: String): ResponseEntity<StreamingResponseBody?> {
        var status = HttpStatus.OK

        val stream = StreamingResponseBody { out: OutputStream? ->

            val time = MeasureTools.measureTimeInMillis {
                val file = File("$storePath/src/$path/$stream/$file")
                if (file.exists()) {
                    var inputStream: InputStream? = null

                    try {
                        val inputStream: InputStream = FileInputStream(file)
                        val bytes = ByteArray(4096)
                        var length: Int? = 0

                        val time1 = MeasureTools.measureTimeInMillis {
                            while (inputStream.read(bytes).also { length = it } >= 0) {
                                out?.write(bytes, 0, length!!)
                                out?.flush()
                            }
                        }

                        monitoring.flushTSChunkDownloadingLatency(time1)
                    } catch (e: Exception) {
                        when(e) {
                            is IOException, is ClientAbortException -> {}
                            else -> throw e
                        }
                    } finally {
                        inputStream?.close()
                    }
                } else {
                    throw VideoNotFoundException()
                }
            }

            monitoring.flushTSChunkResponseLatency(time)

        }

        return ResponseEntity<StreamingResponseBody?>(stream, status)
    }

    @GetMapping(value = ["/test"], produces = [MediaType.TEXT_PLAIN_VALUE])
    @Timed(value = "front.test", longTask = true)
    @ResponseBody
    @Throws(IOException::class)
    fun getP(): String {

            return "x"
    }

    @GetMapping(value = ["/media/{path}/{user}/{stream}/{file}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    @ResponseBody
    @Throws(IOException::class)
    fun getStream(@PathVariable("path") path: String, @PathVariable("user") user: String, @PathVariable("stream") stream: String, @PathVariable("file") file: String): ByteArray? {
        var res: ByteArray? = null;
        try {
            res = services.getFileStream(user,  "src/" + path + "/"+ stream, file)
        } catch (e: Exception) {
            try {
                Thread.sleep(1000)
                res = services.getFileStream(user, path, file)
            } catch (e1: Exception) {

            }
        }

        return res
    }
}