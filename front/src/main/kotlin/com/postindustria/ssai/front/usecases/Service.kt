package com.postindustria.ssai.front.usecases


import com.postindustria.ssai.common.model.PlayStreamEvent
import com.postindustria.ssai.common.model.SSAISession
import com.postindustria.ssai.front.messaging.RunWorkerTask
import com.postindustria.ssai.front.messaging.gcp.GCPMessageBroker
import com.postindustria.ssai.common.model.Stream
import com.postindustria.ssai.common.model.WorkerMetadata
import com.postindustria.ssai.common.model.sessions.Session
import com.postindustria.ssai.front.repository.Repository
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.util.*
import java.util.concurrent.Semaphore
import javax.annotation.PostConstruct

@Service()
class Services {
    @Autowired
    lateinit var session: SSAISession

    @Autowired
    private lateinit var repository: Repository

    @Autowired
    private lateinit var messageBroker: GCPMessageBroker

    @Value("\${front.messaging.worker.response.timeout}")
    lateinit var workerResponseTimeout: String

    var runWorkerTasks = Hashtable<String, RunWorkerTask>()

    @PostConstruct
    fun init() {
        messageBroker.setWorkerOnResultCallback {

            println("worker is started: $it")

            val task = runWorkerTasks[it.target_url.toString()]

            with(task) {
                if(it.state == WorkerMetadata.STATE.ERROR) {
                    task?.hasError = true
                }

                val stream = this?.stream ?: null

                if(stream != null) {
                    releaseLock(stream)
                }
            }

        }
    }

    fun getRunWorkerTask(stream: Stream): RunWorkerTask? {
        var task = runWorkerTasks[stream.target_url.toString()]

        if(task == null) {
            task = RunWorkerTask(stream).apply {
                this.stream = stream
                this.lock = Semaphore(1).apply { acquire() }
            }

            runWorkerTasks[stream.target_url.toString()] = task
        }

        return task
    }

    fun releaseLock(stream: Stream): Unit {
        val runWorkerTask = getRunWorkerTask(stream)

        runWorkerTask?.let {
            runWorkerTask?.lock?.release()
            runWorkerTasks.remove(runWorkerTask.stream?.target_url.toString())
        }
    }

    fun getStream(url: String): Stream? {
        var stream: Stream = repository.getStreamBySource(url)?.let {
            it
        } ?: repository.saveStream(Stream(url))?.let {
           it
        }

        repository.updateStreamState(com.postindustria.ssai.common.model.sessions.Stream(
                stream.target_url.toString(),
                stream.target_url.toString())
        )

        return getStreamFromWorker(stream)
    }

    fun getStreamFromWorker(stream: Stream): Stream? {
        var res: Stream? = null

        val task = GlobalScope.async {
            val task = getRunWorkerTask(stream)
            val workerMetadata = repository.getWorkerState(stream)

            if(workerMetadata?.isPresent!! && workerMetadata.get().state == WorkerMetadata.STATE.LIVE ) {
                return@async stream
            } else
                repository.updateWorkerState(WorkerMetadata(stream.target_url.toString()))

                stream?.let {
                    println("run worker:$it")
                    messageBroker.sendNewWorkerMessage(PlayStreamEvent().apply {
                        this.stream = it
                        this.session = SSAISession().apply { id = session?.id }
                    })
                }

                task?.lock?.acquire()

            return@async if(task?.hasError == false) task?.stream else null
        }

        runBlocking {
            res = withTimeoutOrNull(workerResponseTimeout.toLong()) { task.await() } // wait with timeout }
        }

        res?.let {

        } ?: releaseLock(stream)

        return res
    }

    fun getFileStream(user: String, path: String, file: String): ByteArray? = repository.getWorkerState(path)
        ?.filter { it.state == WorkerMetadata.STATE.LIVE }?.let {
            repository.getFileStream("$path/$user/", file)
        } ?: null

    fun updateSessionState(streamId: String, sessionId: String) {
        repository.putSession(streamId, sessionId)
    }
}