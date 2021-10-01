package com.jetbrains.handson.chat.server

import io.ktor.http.cio.websocket.*
import java.util.concurrent.atomic.AtomicInteger

class Connection(val session: DefaultWebSocketSession, userName: String?) {
    companion object {
        var lastId = AtomicInteger(0)
    }
    val name = userName ?: "user${lastId.getAndIncrement()}"
}