package com.postindustria.ssai.splitter.messaging

import com.postindustria.ssai.loader.messaging.MessageBroker
import org.springframework.beans.factory.annotation.Autowired

public class MessagesHandler{
    @Autowired
    lateinit var messageBroker: MessageBroker
}