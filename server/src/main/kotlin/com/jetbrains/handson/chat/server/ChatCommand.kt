package com.jetbrains.handson.chat.server

/**
 * Models a chat command that a user can issue.
 */
fun interface ChatCommand {
    suspend fun runCommand(): Unit
}