package com.postindustria.ssai.streaming.mvc

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "file not found")
class VideoNotFoundException : RuntimeException()