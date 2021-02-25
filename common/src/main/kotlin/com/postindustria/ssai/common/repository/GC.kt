package com.postindustria.ssai.common.repository

import com.postindustria.ssai.common.model.log
import org.apache.tomcat.util.http.fileupload.FileUtils
import java.io.File
import java.io.IOException

class GC {
    companion object {
        fun clearStorage(path: String) {
            val f = File(path)
            if (f.exists()) {
                try {
                    FileUtils.deleteDirectory(f)
                } catch (e: IOException) {
                    log.error("Can't clean storage", e)
                }
            }
        }
    }
}