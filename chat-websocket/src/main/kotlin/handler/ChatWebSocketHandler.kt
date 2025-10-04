package com.chat.websocket.handler

import com.chat.domain.service.ChatService
import com.chat.persistence.service.WebSocketSessionManager
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession


@Component
class ChatWebSocketHandler(
    private val sesssion: WebSocketSessionManager,
//    private val messageService: WebSocketMessageService,
    private val chatService: ChatService,
    private val objectMapper: ObjectMapper
    ): WebSocketHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = getUserIdFromSession(session)

        if(userId == null){
            logger.warn("WebSocket connection failed: no userId")
            session.close(CloseStatus.BAD_DATA)
            return
        }



    }

    override fun handleMessage(
        session: WebSocketSession,
        message: WebSocketMessage<*>
    ) {
        TODO("Not yet implemented")
    }

    override fun handleTransportError(
        session: WebSocketSession,
        exception: Throwable
    ) {
        TODO("Not yet implemented")
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        closeStatus: CloseStatus
    ) {
        TODO("Not yet implemented")
    }

    override fun supportsPartialMessages(): Boolean {
        TODO("Not yet implemented")
    }

    private fun getUserIdFromSession(session: WebSocketSession): Long? {
        return session.attributes["userId"] as? Long
    }

}
