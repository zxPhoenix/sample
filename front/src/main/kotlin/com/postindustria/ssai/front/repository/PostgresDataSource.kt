package com.postindustria.ssai.front.repository;

import com.postindustria.ssai.common.model.Stream;
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository;

@org.springframework.stereotype.Repository()
interface PostgresDataSource : CrudRepository<Stream, Long> {
    @Query(value = "SELECT r FROM Stream r Where r.source_url = :source")
    fun findBySource(source: String): Stream?
    @Query(value = "SELECT r FROM Stream r Where r.target_url = :target")
    fun findByTarget(target: String): Stream?
}
