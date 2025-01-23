package io.github.bionictigers.web

import android.content.Context
import commands.Scheduler
import io.javalin.Javalin
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsContext
import java.io.File

object Server {
    fun start() {
        val app = Javalin.create { //config ->
            //Add static files here
        }.start(10464)

        val sessions = mutableListOf<WsContext>()

        app.ws("/updates") { ws: WsConfig ->
            ws.onConnect { ctx ->
                sessions.add(ctx)
                println("Client connected: ${ctx.sessionId}")
            }

            ws.onClose { ctx ->
                sessions.remove(ctx)
                println("Client disconnected: ${ctx.sessionId}")
            }

            ws.onMessage { ctx ->
                println("Received from client: ${ctx.message()}")
            }
        }

        Thread {
            while (true) {
                // This is just a simulation of server-side events
                Thread.sleep(100)
                val updates = Scheduler.serialize()
                sessions.forEach {
                    if (it != null) it.send(updates)
                }
            }
        }.start()
    }
}