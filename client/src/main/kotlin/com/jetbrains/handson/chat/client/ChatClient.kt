package com.jetbrains.handson.chat.client

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.*

@KtorExperimentalAPI
fun main() {
    val client = HttpClient {
        install(WebSockets)
    }
    runBlocking {
        client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/chat") {
            val messageOutputRoutine = launch { outputMessages() }
            val userInputRoutine = launch { inputMessages() }
            userInputRoutine.join()
            messageOutputRoutine.cancelAndJoin()
        }
    }
    client.close()
    println("Connection closed! Goodbye!")
}

suspend fun DefaultWebSocketSession.outputMessages() {
   try {
       for(message in incoming) {
           message as? Frame.Text ?: continue
           println(message.readText())
       }
   } catch(e: Exception) {
       println("Error while receiving: ${e.localizedMessage}")
   }
}

suspend fun DefaultWebSocketSession.inputMessages() {
    while(true) {
        val message = readLine()
        if("exit".equals(message, true)) return
        try {
            if(message!= null) send(message)
        } catch (e: Exception) {
            println("Error while sending: ${e.localizedMessage}")
        }
    }
}
