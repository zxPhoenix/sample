package com.postindustria.ssai.front.mvc

import com.postindustria.ssai.common.model.SSAISession
import com.postindustria.ssai.common.monitoring.MeasureTools
import com.postindustria.ssai.common.monitoring.Monitoring
import com.postindustria.ssai.front.usecases.IdentificationService
import com.postindustria.ssai.front.usecases.Services
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import java.io.IOException
import java.net.URLDecoder
import javax.servlet.http.HttpServletRequest

@RestController
class RESTController {
    @Autowired
    lateinit var services: Services

    @Autowired
    lateinit var monitoring: Monitoring

    @Autowired
    lateinit var session: SSAISession

    @GetMapping("/play")
    fun play(request: HttpServletRequest): RedirectView? {
        var res: RedirectView? = null

        val time = MeasureTools.measureTimeInMillis {
             res = services.getStream(URLDecoder.decode(request.queryString?.replace("source=", "")!!, "UTF-8"))?.let{
                RedirectView("media/${it.target_url}/" + session.id +"/master.m3u8" ?: "/")
            } ?: RedirectView("media/no_result")
        }

        /*GlobalScope.launch {
            monitoring.flushMasterPlaylistLatency(time)
        }*/

        return res
    }

    @GetMapping(value = ["/media/{path}/{user}/{file}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    @ResponseBody
    @Throws(IOException::class)
    fun getFile(@PathVariable("path") path: String, @PathVariable("user") user: String, @PathVariable("file") file: String): ByteArray? {
        var res: ByteArray? = null;
        try {
            services.updateSessionState(path, user)
            res = services.getFileStream(user, path, file)
        } catch (e: Exception) {
            try {
                Thread.sleep(3000)
                res = services.getFileStream(user, path, file)
            } catch (e1: Exception) {

            }
        }

        return res
    }
}