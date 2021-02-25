package com.postindustria.ssai.common.model

import com.fasterxml.jackson.annotation.JsonProperty

class StreamIsAliveEvent( @JsonProperty("target_url")
                          var target_url: String?)