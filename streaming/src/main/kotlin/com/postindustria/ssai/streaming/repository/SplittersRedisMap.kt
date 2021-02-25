package com.postindustria.ssai.streaming.repository;

import com.postindustria.ssai.common.model.WorkerMetadata
import org.springframework.data.repository.CrudRepository;

@org.springframework.stereotype.Repository()
interface SplittersRedisMap : CrudRepository<WorkerMetadata, String> {}
