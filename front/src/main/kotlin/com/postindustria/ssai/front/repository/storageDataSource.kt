package com.postindustria.ssai.front.repository

import org.springframework.beans.factory.annotation.Value
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class StorageDataSource {
    @Value("\${worker.store}")
    lateinit var storePath: String

    fun getFileStream(path: String, file: String): ByteArray? {
        var input: InputStream? = null
        var res : ByteArray? = null

        try {
            input = FileInputStream("$storePath$path/$file")
        } finally {
            try {
                res = input?.readBytes()
            } catch (e: IOException) {

            } finally {
                try {
                    input?.close()
                } catch (e1: Exception ) {

                }
            }
        }

        return res
    }
}