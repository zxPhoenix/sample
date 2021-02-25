package com.postindustria.ssai.front.repository;

import com.postindustria.ssai.common.model.WorkerMetadata
import org.springframework.data.repository.CrudRepository;

@org.springframework.stereotype.Repository()
interface RedisDataSource : CrudRepository<WorkerMetadata, String> {}
