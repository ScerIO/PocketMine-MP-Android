package io.scer.pocketmine.server

interface ServerEventsHandler {
    fun error(type: ServerError, message: String?)
    fun stop()
    fun start()
}