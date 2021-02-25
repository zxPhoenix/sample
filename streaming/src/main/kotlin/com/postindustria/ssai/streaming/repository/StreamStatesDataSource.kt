package com.postindustria.ssai.streaming.repository;

import com.postindustria.ssai.common.model.sessions.Stream
import org.springframework.data.repository.CrudRepository;

@org.springframework.stereotype.Repository()
interface StreamStatesDataSource : CrudRepository<Stream, String> {
    fun findByTargetUrl(value: String): Stream
}
