package io.github.bionictigers.axiom.core.web

import com.qualcomm.robotcore.util.RobotLog
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.bionictigers.axiom.core.scheduler.Scheduler
import fi.iki.elonen.NanoWSD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response.Status
import fi.iki.elonen.NanoWSD.WebSocket
import fi.iki.elonen.NanoWSD.WebSocketFrame
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

@JsonClass(generateAdapter = true)
sealed class IncomingMessage {
    abstract val type: String

    @JsonClass(generateAdapter = true)
    data class Edit(
        override val type: String = "edit",
        val path: String,
        val value: Any
    ) : IncomingMessage()
}

interface Serializable {
    fun serialize(tick: Long): Map<String, Any?>
}

object Server {
    // Keep track of active WebSocket connections
    private val connections = mutableListOf<UpdatesWebSocket>()

    private val newConnectionCallbacks: List<((Serializable) -> Unit) -> Unit> = mutableListOf()

    // Moshi instance configured with the Kotlin adapter
    private val moshi: Moshi = Moshi.Builder()
        .add(PolymorphicJsonAdapterFactory.of(IncomingMessage::class.java, "type")
            .withSubtype(IncomingMessage.Edit::class.java, "edit"))
        .add(KotlinJsonAdapterFactory())
        .build()

    @OptIn(ExperimentalStdlibApi::class)
    private val anyAdapter = moshi.adapter<Any>()
    @OptIn(ExperimentalStdlibApi::class)
    private val messageAdapter = moshi.adapter<IncomingMessage>()

    @JvmStatic
    internal fun start() {
        println("Start Called")
        // Create & start the NanoWSD server on port 10464
        val server = object : NanoWSD(10464) {
            override fun openWebSocket(handshake: IHTTPSession): WebSocket {
                // Called when a new WebSocket client connects to any path.
                return UpdatesWebSocket(handshake)
            }

            override fun serveHttp(session: IHTTPSession): Response {
                // Fallback for non-WebSocket HTTP requests.
                return newFixedLengthResponse(Status.OK, "text/plain", "NanoHTTPD on /")
            }
        }
        server.start()
        RobotLog.dd("Axiom", "NanoWSD server started on port 10464")
    }

    internal fun send(data: Serializable) {
        val update = data.serialize(Scheduler.tick)
        val updateJson = try {
            anyAdapter.toJson(update)
        } catch (e: Exception) {
            RobotLog.dd("Axiom", "Serialization error: ${e.message}")
            "{}"
        }


        synchronized(connections) {
            connections.forEach { ws ->
                ws.sendSafe(updateJson)
            }
        }
    }

    fun onNewConnection(callback: ((data: Serializable) -> Unit) -> Unit) {
        (newConnectionCallbacks as MutableList).add(callback)
    }

    private class UpdatesWebSocket(handshakeRequest: IHTTPSession) : WebSocket(handshakeRequest) {
        private val job = SupervisorJob()
        private val serverScope = CoroutineScope(job + Dispatchers.IO)

        private val outMessages = Channel<String>(capacity = 64)

        override fun onOpen() {
            RobotLog.dd("Axiom", "Client connected: ${handshakeRequest.remoteIpAddress}")
            synchronized(connections) {
                connections.add(this)
                newConnectionCallbacks.forEach {
                    it { data ->
                        sendSafe(
                            anyAdapter.toJson(data.serialize(Scheduler.tick))
                        )
                    }
                }
            }

            serverScope.launch {
                outMessages.consumeEach {
                    sendSafe(it)
                }
            }

            serverScope.launch {
                while (isActive) {
                    try { ping(ByteArray(0)) } catch (_: IOException) { break }
                    delay(2500)
                }
            }
        }

        override fun onClose(
            code: CloseCode?,
            reason: String?,
            initiatedByRemote: Boolean
        ) {
            RobotLog.dd("Axiom", "Client disconnected: ${handshakeRequest.remoteIpAddress} (code=$code, reason=$reason)")

            synchronized(connections) {
                connections.remove(this)
            }
            outMessages.close()
            job.cancel()
        }

        override fun onMessage(message: WebSocketFrame?) {
            val text = message?.textPayload ?: return
            // Parse & handle off the websocket thread
            serverScope.launch(Dispatchers.Default) {
                try {
                    when (val msg = messageAdapter.fromJson(text)) {
                        is IncomingMessage.Edit -> Scheduler.edit(msg.path, msg.value.toString())
                        null -> RobotLog.dd("Axiom", "Null message after parse")
                    }
                } catch (e: Exception) {
                    RobotLog.ww("Axiom", "Parse fail: ${e.message}")
                }
            }
        }

        override fun onPong(pong: WebSocketFrame?) {}

        override fun onException(exception: IOException?) {
            RobotLog.ww("Axiom", "WebSocket Exception: ${exception?.message}")
            // Gracefully close the connection when an exception occurs.
            try {
                close(CloseCode.InternalServerError, "Handler terminated due to exception", false)
            } catch (e: Exception) {
                RobotLog.ww("Axiom", "Failed to close WebSocket: ${e.message}")
            }
        }

        /** Helper method to send text safely, ignoring exceptions */
        fun sendSafe(text: String) {
            try {
                send(text)
            } catch (e: IOException) {
                RobotLog.ww("Axiom", "Failed to send to client: ${e.message}")
            }
        }
    }
}
