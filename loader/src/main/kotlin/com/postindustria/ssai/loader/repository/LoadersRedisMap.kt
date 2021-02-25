package com.postindustria.ssai.loader.repository;

import com.postindustria.ssai.common.model.LoaderMetadata
import com.postindustria.ssai.common.model.WorkerMetadata
import org.springframework.data.repository.CrudRepository;

@org.springframework.stereotype.Repository()
interface LoadersRedisMap : CrudRepository<LoaderMetadata, String> {}
