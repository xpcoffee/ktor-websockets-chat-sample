package com.jetbrains.handson.chat.server

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.*
import kotlin.collections.LinkedHashSet

/**
 * Entry point for the chat server
 */
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    install(CORS) {
        anyHost()
        method(HttpMethod.Options)
    }
    install(WebSockets)
    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        val commandHandler = CommandHandler(connections)

        /**
         * Primary chat path
         */
        webSocket("/chat") {
            val userName = promptUserName(this, incoming)
            val thisConnection = Connection(this, userName)
            connections += thisConnection

            try {
                send("You are connected! There are ${connections.count()} users here.")
                for(frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val userInput = frame.readText()
                    commandHandler.handle(thisConnection, userInput)
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("Removing $thisConnection!")
                connections -= thisConnection
            }
        }
    }
}

/**
 * Prompts a new chat user for their username
 */
suspend fun promptUserName(session: WebSocketSession, incoming: ReceiveChannel<Frame>): String? {
    session.send("Please enter a username:")
    return (incoming.receive() as? Frame.Text)?.readText()
}