package com.jetbrains.handson.chat.server

import io.ktor.http.cio.websocket.*

/**
 * Parses user inputs for commands and executes them accordingly.
 */
class CommandHandler(private val connections: MutableSet<Connection>) {
    suspend fun handle(connection: Connection, userInput: String) = run {
        val command = with(userInput) {
            when {
                isNullOrBlank() -> buildNoOpChatCommand()
                startsWith("/") -> getCommand(connection, userInput)
                else -> buildGeneralChatCommand(connection, userInput)
            }
        }
        command.runCommand()
    }

    /**
     * Build the appropriate command or returns an error to the chat user
     */
    private fun getCommand(connection: Connection, userInput: String): ChatCommand {
        val tokens = userInput.split(" ")
        val command = tokens.take(1).firstOrNull()
        val commandArgs = tokens.drop(1)

        return when(command) {
            "/whisper" -> buildWhisperChatCommand(connection, commandArgs)
            else -> buildChatCommandError(connection, "unrecognized command $command")
        }
    }

    /**
     * Allows users to talk directly to one another
     */
    private fun buildWhisperChatCommand(connection: Connection, arguments: List<String>): ChatCommand {
        val recipient = arguments.firstOrNull() ?: ""

        if(!recipient.startsWith("@")) {
            return buildChatCommandError(connection, "You must specify a recipient using @userName")
        }

        val recipientConnection = connections.find { conn -> "@${conn.name}" == recipient }
            ?: return buildChatCommandError(connection, "User $recipient is not online")

        val text = "[${connection.name}] ${arguments.drop(1).joinToString(" ")}"
        return ChatCommand {
            connection.session.send(text)
            recipientConnection.session.send(text)
        }
    }

    /**
     * Sends a message to all chat users
     */
    private fun buildGeneralChatCommand(connection: Connection, userInput: String): ChatCommand {
       val text = "[${connection.name}] $userInput"
       return ChatCommand {
           for(conn in connections) {
               conn.session.send(text)
           }
       }
    }

    /**
     * Returns an error message to the chat user that issued the command
     */
    private fun buildChatCommandError(connection: Connection, message: String): ChatCommand {
        return ChatCommand {
            connection.session.send("Error: $message")
        }
    }

    /**
     * Does nothing
     */
    private fun buildNoOpChatCommand(): ChatCommand {
        return ChatCommand {}
    }
}
