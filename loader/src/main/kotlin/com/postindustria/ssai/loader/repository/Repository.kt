package com.postindustria.ssai.loader.repository

import com.postindustria.ssai.common.model.LoaderMetadata
import com.postindustria.ssai.common.model.log
import com.postindustria.ssai.loader.loader.M3U8Loader
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.tomcat.util.http.fileupload.FileUtils
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import java.io.*
import java.net.URL
import java.nio.file.FileSystemException
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


@Repository
class Repository {
    @Autowired
    lateinit var db: PostgresDataSource

    @Autowired
    lateinit var loadersMap: LoadersRedisMap

    @Value("\${worker.store}")
    lateinit var store: String

    fun loadAsString(url: String): String? {
        var result: String? = null

        log(M3U8Loader.TAG).info("Load M3U8 - {}", url)
        val client = OkHttpClient()
        val request = Request.Builder()
                .method("GET", null)
                .url(url)
                .build()

        val response = client.newCall(request).execute()
        result = response.body?.string()
        log(M3U8Loader.TAG).debug("playlist response - {}", result);

        if (!response.isSuccessful) {
            throw IOException("Unexpected code $response")
        }

        return result
    }

    fun loadFromNet(url: String, filename: String){
        val client = OkHttpClient()
        var request = Request.Builder().url(url)
                .addHeader("Content-Type", "application/json")
                .build()
        val response =  OkHttpClient().newCall(request).execute()

        val `in`: InputStream? = response?.body?.byteStream()

        java.nio.file.Files.copy(
                `in`,
                Paths.get(store + "/"  +filename),
                StandardCopyOption.REPLACE_EXISTING);

        IOUtils.closeQuietly(`in`)

        response?.body?.close()
    }

    fun getPathForUrl(url: String) = Paths.get(URL("file://host/" + url).path).parent.toString()

    fun saveToFile(url: String, content: String){
        var path: String = this.checkFolder(this.store + getPathForUrl(url))

        val fos = FileOutputStream(path + "/" + Paths.get(url).fileName, false)
        fos.write(content.toByteArray())
        fos.flush()
        fos.close()
    }

    @Throws(FileSystemException::class)
    fun checkFolder(path: String): String {
        var backup = File(path)
        if (!backup.exists()) {
            backup.mkdir()
        }

        if (!backup.exists()) {
            throw FileSystemException("Stream store folder not exist: " + backup.path)
        }

        return path
    }

    fun getFileStream(path: String, file: String): ByteArray? {
        var input: InputStream? = null
        var res : ByteArray? = null

        try {
            input = FileInputStream("$store$path/$file")
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

    fun createStremFolderIfNotExists(url: String): String? {
        clearPath("$store/src/$url")
        checkFolder("$store")
        checkFolder("$store/src")
        checkFolder("$store/src/$url")
        return url
    }

    fun clearPath(path: String?) {
        val f = File(path)
        if (f.exists()) {
            try {
                FileUtils.deleteDirectory(f)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun updateLoaderState(metadata: LoaderMetadata) {
        loadersMap.save(metadata)
    }
    fun getLoaderState(key: String?) = key?.let {
        loadersMap.findById(it)
    } ?: null
}