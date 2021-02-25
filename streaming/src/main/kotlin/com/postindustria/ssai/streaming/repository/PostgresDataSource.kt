package com.postindustria.ssai.streaming.repository;

import com.postindustria.ssai.common.model.Stream;
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository;
import java.util.*

@org.springframework.stereotype.Repository()
interface PostgresDataSource : CrudRepository<Stream, Long> {
    @Query(value = "SELECT r FROM Stream r Where r.source_url = :source")
    fun findBySource(source: String): Stream?
    @Query(value = "SELECT r FROM Stream r Where r.target_url = :target")
    fun findByTarget(target: UUID): Stream?
}
