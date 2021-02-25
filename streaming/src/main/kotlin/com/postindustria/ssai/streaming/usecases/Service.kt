package com.postindustria.ssai.streaming.usecases


import com.fasterxml.jackson.databind.util.LRUMap
import com.postindustria.ssai.common.model.*
import com.postindustria.ssai.streaming.messaging.LoadStreamTask
import com.postindustria.ssai.streaming.messaging.gcp.GCPMessageBroker
import com.postindustria.ssai.streaming.repository.Repository
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.integration.support.locks.LockRegistry
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PostConstruct

@Service()
class Services {
    private var cacheStream = LRUMap<String, Stream>(0, 2000)

    @Autowired
    lateinit var session: SSAISession

    @Autowired
    private lateinit var repository: Repository

    @Autowired
    private lateinit var messageBroker: GCPMessageBroker

    @Value("\${front.messaging.worker.response.timeout}")
    lateinit var workerResponseTimeout: String

    @Autowired
    lateinit var lockRegistry: LockRegistry

    val tasks = InitStreamTasks()

    companion object {
        var lock = ReentrantLock()
    }

    @PostConstruct
    fun init() {
        messageBroker.setSplitterOnResultCallback {

            println("splitter is started: $it")

            val task = tasks.get(it.id!!)

            if(task != null) {
                with(task) {
                    if(it.state == WorkerMetadata.STATE.ERROR) {
                        task?.hasError = true
                    }

                    val stream = this?.stream!!

                    if(stream != null) {
                        tasks.release(it.id!!, true)
                    }
                }
            }
        }

        messageBroker.setLoaderOnResultCallback {
            println("loader is started: $it")

            val task = tasks.get(it.request_id!!)

            task?.let { t->
                with(t) {
                    if(it.state == LoaderMetadata.STATE.ERROR) {
                        task?.hasError = true
                    }

                    val stream = this?.stream!!

                    if(stream != null) {
                        tasks.release(it.request_id!!, false)
                    }
                }
            }
        }
    }

    fun getStream(id: String, sessionId: String?): Stream? {
        var stream: Stream? = null;

        try {
            if(cacheStream[id] != null) {
                stream = cacheStream[id]
            } else {
                synchronized(lock) {
                    if(cacheStream != null) {
                        stream = cacheStream[id]
                    }

                    stream = repository.getStreamByTarget(id)?.let {
                        cacheStream.put(id, it)
                        it
                    } ?: null
                }
            }
        } catch (e: Exception) {
            if(e !is IllegalArgumentException) {
                log.error("can't get stream from DB", e)
            }
        }

        var playStreamEvent = PlayStreamEvent().apply {
            this.stream = stream
            this.session = SSAISession().apply { this.id = sessionId }
        }

        if(stream != null) {
            return loadStream(playStreamEvent)
        }

        return null
    }

    fun loadStream(playStreamEvent: PlayStreamEvent): Stream? {
        var res: Stream? = null

        val task = GlobalScope.async {
            val task = tasks.get(playStreamEvent)
            var splitterMetadata = repository.getSplitterState(getKey(playStreamEvent))

            if (splitterMetadata?.isPresent!! && splitterMetadata.get().state == WorkerMetadata.STATE.LIVE) {
            } else

                lockRegistry.obtain(getKey(playStreamEvent)).lock()

                splitterMetadata = repository.getSplitterState(getKey(playStreamEvent))
                if (splitterMetadata?.isPresent!! && splitterMetadata?.get()?.state == WorkerMetadata.STATE.LIVE) {

                    try {
                        lockRegistry.obtain(getKey(playStreamEvent)).unlock()
                    } catch (e: Exception) { }

                    return@async playStreamEvent.stream
                } else {
                    initLoaderIfNeeded(task!!, playStreamEvent)

                    repository.updateSplitterState(WorkerMetadata(id = getKey(playStreamEvent)))

                    playStreamEvent?.stream?.let {
                        println("run splitter:$it")
                        messageBroker.sendNewSplitterEvent(playStreamEvent)
                    }

                    task?.lock?.acquire()

                    try {
                        lockRegistry.obtain(getKey(playStreamEvent)).unlock()
                    } catch (e: Exception) { }
                }


                return@async if(task?.hasError == false) task?.stream else null
            }

        runBlocking {
            res = withTimeoutOrNull(workerResponseTimeout.toLong()) { task.await() } // wait with timeout }
        }

        res?.let {

        } ?: tasks.release(getKey(playStreamEvent), true)

        return res
    }

    private fun initLoaderIfNeeded(task: LoadStreamTask, playStreamEvent: PlayStreamEvent) {
        var workerMetadata = repository.getLoaderState(getLoaderKey(playStreamEvent))

        if(!workerMetadata?.isPresent!! || workerMetadata.get().state != LoaderMetadata.STATE.LIVE) {
            lockRegistry.obtain(getLoaderKey(playStreamEvent)).lock()

            workerMetadata = repository.getLoaderState(getLoaderKey(playStreamEvent))
            if(!workerMetadata?.isPresent!! || workerMetadata?.get()?.state != LoaderMetadata.STATE.LIVE) {
                repository.updateLoaderState(LoaderMetadata(id=getLoaderKey(playStreamEvent)))

                playStreamEvent?.stream?.let {
                    println("run loader:$it")
                    messageBroker.sendNewLoaderEvent(playStreamEvent)
                }

                task?.lock?.acquire()
            }

            lockRegistry.obtain(getLoaderKey(playStreamEvent)).unlock()
        }
    }

    fun getFileStream(user: String, path: String, file: String): ByteArray? = /*repository.getWorkerState(path)
        ?.filter { it.state == WorkerMetadata.STATE.LIVE }?.let {
           *//* GlobalScope.launch {
                messageBroker.sendStreamIsAliveMessage(path)
            }*//*

            repository.getFileStream("$path/$user/", file)
        } ?: null*/
            repository.getFileStream("$path/", file)

    fun getFileStream(path: String, file: String): ByteArray? = repository.getFileStream("$path/", file)

    fun updateSessionState(streamId: String, sessionId: String) {
        repository.putSession(streamId, sessionId)
    }

    private fun getKey(event: PlayStreamEvent) = InitStreamTasks.getKey(event)
    private fun getLoaderKey(event: PlayStreamEvent) = "Loader:" + event.stream?.target_url
    private fun getLoaderKey(id: String) = "Loader:" + id.split("::::")[0]
}