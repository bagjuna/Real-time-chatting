package com.chat.websocket.interceptor

import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor


@Component
class WebSocketHandshakeInterceptor : HandshakeInterceptor {
    private val logger = LoggerFactory.getLogger(WebSocketHandshakeInterceptor::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        return try {
            // ws://localhost:8080/chat?userId=123
            val uri = request.uri
            val query = uri.query

            if(query != null){
                val param = parseQuery(query)
                val userId = param["userId"]?.toLongOrNull()

                if (userId != null) {
                    attributes["userId"] = userId
                    true
                } else {
                    return false
                }
            } else{
                return false
            }

            true
        } catch (e: Exception) {
            logger.error("WebSocket HandshakeInterceptor beforeHandshake error", e)
            false
        }

    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
        if (exception != null) {
            logger.error("WebSocket HandshakeInterceptor exception", exception)
        } else {
            logger.info("WebSocket HandshakeInterceptor")
        }
    }


    // query 파싱 함수
    // "userId=123&token=abc" -> mapOf("userId" to "123", "token" to "abc")
    private fun parseQuery(query: String) : Map<String, String> {
        return query.split("&")
            .mapNotNull {
                val parts = it.split("=")
                if (parts.size == 2) {
                    parts[0] to parts[1]
                } else {
                    null
                }
            }.toMap()
    }
}
