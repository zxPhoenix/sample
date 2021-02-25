package com.postindustria.ssai.loader.common

import com.github.kokorin.jaffree.StreamType
import com.github.kokorin.jaffree.ffprobe.Program
import com.postindustria.ssai.lib.VideoProfiles

data class Bitrates(val video: Long, val audio: Long)

class VideoTools {
    companion object {
        fun calcBitrates(program: Program): Bitrates {
            var videoBitrate: Long = -1
            var audioBitrate: Long = -1

            val exclude = mutableListOf<Int>()

            var i = 0;
            program.section.getSections("stream").filter{ it.getStreamType("codec_type") == StreamType.VIDEO }?.let {it ->
                if(it.isEmpty()) {
                    videoBitrate = -1
                } else {
                    with(it[0]){
                        val framerate = this.getString("r_frame_rate").split("/")?.get(0).toInt()
                        val width = this.getLong("width")
                        val bitsPerPixel = this.getInteger("bits_per_raw_sample")

                        var profiles = VideoProfiles().profileMatrix.filter { item -> item[0] == width }
                        if(profiles.isEmpty()) {
                            exclude.add(i)
                        }
                        videoBitrate = if(profiles?.isNotEmpty()) profiles.get(0).get(3) else -1 // pixels * framerate * (if(bitsPerPixel == 8) 1 else bitsPerPixel/8) / 1000
                    }
                }
                i++
            }

            i = 0
            program.section.getSections("stream").filter { it.getStreamType("codec_type") == StreamType.AUDIO }?.let {
                if(it.isEmpty()) {
                    audioBitrate = -1
                } else with(it[0]){
                    audioBitrate = this.getLong("sample_rate")
                }

                i++
            }

            return Bitrates(videoBitrate, audioBitrate)
        }
    }
}