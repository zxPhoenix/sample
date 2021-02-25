package com.postindustria.ssai.loader.loader

import com.postindustria.ssai.common.model.log
import com.postindustria.ssai.lib.M3u8Playlist
import com.postindustria.ssai.loader.repository.Repository
import org.springframework.beans.factory.annotation.Autowired
import java.net.URL

class M3U8Loader {
    @Autowired
    private lateinit var repository: Repository

    companion object {
        val TAG = M3U8Loader.javaClass.name!!
    }

    private fun load(url: String): M3u8Playlist? {
        var playlist: M3u8Playlist? = null

        try {
            repository.loadAsString(url)?.let {
                playlist = M3u8Playlist(URL(url), it)
            }
        } catch (e: Exception) {
            log(TAG).error("Can't load M3U8", url)
        }

        return playlist
    }

    fun loadMasterPlayList(url: String): M3u8Playlist? = load(url)

    fun loadMediaPlayList(url: String): M3u8Playlist? = load(url)

    fun loadMediaPlayLists(urls: List<String>?): List<M3u8Playlist>? {
        var playlists = mutableListOf<M3u8Playlist>()

        urls?.forEach {
            loadMediaPlayList(it)?.let { pl ->
                playlists.add(pl)
            }
        }

        return playlists
    }
}