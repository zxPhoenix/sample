package com.postindustria.ssai.front.repository;

import com.postindustria.ssai.common.model.sessions.Session
import com.postindustria.ssai.common.model.sessions.Stream
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository

@Repository
interface StreamSessionsDataSource : CrudRepository<Session, String> {
    fun findByStreamId(value: String): List<Session>
}
