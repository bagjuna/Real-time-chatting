package com.chat.websocket.handler

import com.chat.domain.dto.ErrorMessage
import com.chat.domain.dto.SendMessageRequest
import com.chat.domain.model.MessageType
import com.chat.domain.service.ChatService
import com.chat.persistence.service.WebSocketSessionManager
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession


@Component
class ChatWebSocketHandler(
    private val sessionManager: WebSocketSessionManager,
//    private val messageService: WebSocketMessageService,
    private val chatService: ChatService,
    private val objectMapper: ObjectMapper
) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = getUserIdFromSession(session)

        if (userId != null) {
            sessionManager.addSession(userId, session)
            logger.info("Session $userId, established for ${session.id}")
            try {
                loadUserChatRooms(userId)
            }catch (e: Exception) {
                logger.error("Error while loading user chat rooms", e)
            }

        }


    }

    override fun handleMessage(
        session: WebSocketSession,
        message: WebSocketMessage<*>
    ) {
        val userId = getUserIdFromSession(session) ?: return

        try {
            when (message) {
                is TextMessage ->{
                    handleTextMessage(session, userId, message.payload)
                }
                else -> {
                    logger.warn("Unsupported message type: ${message.javaClass.name}")
                }
            }
        }catch (e: Exception) {
            logger.warn("Error while processing message", e)
            sendErrorMessage(session, "메시지 처리 에러")
        }

    }

    override fun handleTransportError(
        session: WebSocketSession,
        exception: Throwable
    ) {
        val userId = getUserIdFromSession(session)

        // EOFException -> 클라이언트 연결 해제, 정상적인 상황 (로그 레벨 따로 두기 위해 )
        if(exception is java.io.EOFException) {
            logger.debug("WebSocket connection closed by client $userId from ${session.id}")
        } else {
            logger.error("Websocket transport error for user $userId from ${session.id}", exception)
        }

        if(userId != null) {
            sessionManager.removeSession(userId, session)
        }
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        closeStatus: CloseStatus
    ) {
        val userId = getUserIdFromSession(session) ?: return
        if(userId != null) {
            sessionManager.removeSession(userId, session)
            logger.info("Session removed for $userId from ${session.id}")
        }
        TODO("Not yet implemented")
    }

    override fun supportsPartialMessages(): Boolean {
        TODO("Not yet implemented")
    }

    private fun getUserIdFromSession(session: WebSocketSession): Long? {
        return session.attributes["userId"] as? Long
    }

    private fun loadUserChatRooms(userId: Long) {
        try {
            val chatRooms = chatService.getChatRooms(userId, PageRequest.of(0, 10))

            chatRooms.content.forEach { room ->
                sessionManager.joinRoom(userId, room.id)
            }

            logger.info("Loaded ${chatRooms.content.size} chat rooms for user $userId")
        } catch (e: Exception) {
            logger.error("Failed to load chat rooms for user $userId", e)

        }

    }

    private fun sendErrorMessage(session: WebSocketSession, errorMessage: String,errorCode: String? = null) {
        try {
            val error = ErrorMessage(
                chatRoomId = null,
                message = errorMessage,
                code = errorCode
            )
            val json = objectMapper.writeValueAsString(error)
            session.sendMessage(TextMessage(json))
        } catch (e: Exception) {
            logger.error("Failed to send error message to session ${session.id}", e)
        }

    }

    private fun extractMessageType(payload: String): String? {
        return try {
            objectMapper.readTree(payload).get("type").asText()
        } catch (e: Exception) {
            logger.error("Error extracting message type from payload: ${e.message}", e)
            null
        }
    }

    private fun handleTextMessage(session: WebSocketSession, userId: Long, payload: String) {
        try {
            val messageType = extractMessageType(payload)

            when (messageType) {
                "SEND_MESSAGE" -> {
                    val jsonNode = objectMapper.readTree(payload)

                    val chatRoomId = jsonNode.get("chatRoomId")?.asLong()
                        ?: throw IllegalArgumentException("chatRoomId is required")
                    val messageTypeText = jsonNode.get("messageType")?.asText()
                        ?: throw IllegalArgumentException("messageType is required")
                    val content = jsonNode.get("content")?.asText()

                    val sendMessageRequest = SendMessageRequest(
                        chatRoomId = chatRoomId,
                        type = MessageType.valueOf(messageTypeText),
                        content = content)

                    chatService.sendMessage(sendMessageRequest, userId)
                    // TODO
                }
                else -> {
                    logger.warn("Unknown message type from user $userId: $messageType")
                    sendErrorMessage(session, "알 수 없는 메시지 타입입니다.", "UNKNOWN_MESSAGE_TYPE")
                }
            }
        }catch (e: Exception) {
            logger.error("Error parsing WebSocket message from user $userId: ${e.message}", e)
            sendErrorMessage(session, "메시지 형식이 올바르지 않습니다.", "INVALID_MESSAGE_FORMAT")
        }
    }
}
