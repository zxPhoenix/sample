package com.postindustria.ssai.front.repository;

import com.postindustria.ssai.common.model.Stream
import com.postindustria.ssai.common.model.WorkerMetadata
import com.postindustria.ssai.common.model.sessions.Session
import org.springframework.beans.factory.annotation.Autowired

class Repository {
    @Autowired
    private lateinit var dbDataSource: PostgresDataSource

    @Autowired
    private lateinit var inMemoryDataSource: RedisDataSource

    @Autowired
    private lateinit var storageDataSource: StorageDataSource

    @Autowired
    private lateinit var streamStatesDataSource: StreamStatesDataSource

    @Autowired
    private lateinit var streamSessionsDataSource: StreamSessionsDataSource

    fun getStreamBySource(source: String) = dbDataSource.findBySource(source)
    fun getStreamByTarget(target: String) = dbDataSource.findByTarget(target)
    fun saveStream(stream: Stream) = dbDataSource.save(stream)

    fun updateWorkerState(metadata: WorkerMetadata) {
        inMemoryDataSource.save(metadata)
    }
    fun getWorkerState(stream: Stream?) = stream?.let {
        inMemoryDataSource.findById(it.target_url.toString())
    } ?: null

    fun getWorkerState(target_url: String) = inMemoryDataSource.findById(target_url)

    fun getFileStream(path: String, file: String): ByteArray? = storageDataSource.getFileStream(path, file)

    fun updateStreamState(stream: com.postindustria.ssai.common.model.sessions.Stream){
        streamStatesDataSource.save(stream)
    }

    fun getSessions(stream: String): List<Session>? {
        return streamSessionsDataSource.findByStreamId(stream)
    }

    fun putSession(stream: String, session: String): Session {
        return streamSessionsDataSource.save(Session(session, stream, System.currentTimeMillis()))
    }
}