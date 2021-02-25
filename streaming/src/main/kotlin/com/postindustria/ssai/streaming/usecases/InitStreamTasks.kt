package com.postindustria.ssai.streaming.usecases

import com.postindustria.ssai.common.model.PlayStreamEvent
import com.postindustria.ssai.streaming.messaging.LoadStreamTask
import java.util.*
import java.util.concurrent.Semaphore

class InitStreamTasks {
    companion object {
        fun getKey(event: PlayStreamEvent) = event.stream?.target_url.toString() + "::::" + event.session?.id
    }

    var loadStreamTasks = Hashtable<String, LoadStreamTask>()

    fun get(event: PlayStreamEvent): LoadStreamTask? {
        val key = getKey(event)
        var task = loadStreamTasks[key]

        if(task == null) {
            task = LoadStreamTask(event.stream!!).apply {
                this.stream = event.stream!!
                this.lock = Semaphore(1).apply { acquire() }
            }

            loadStreamTasks[key] = task
        }

        return task
    }

    fun get(key: String): LoadStreamTask? {
        var task = loadStreamTasks[key]

        return task
    }

    fun release(key: String, remove: Boolean): Unit {
        val loadStreamTask = get(key)

        loadStreamTask?.let {
            loadStreamTask?.lock?.release()
            if(remove) {
                loadStreamTasks.remove(key)
            }
        }
    }
}