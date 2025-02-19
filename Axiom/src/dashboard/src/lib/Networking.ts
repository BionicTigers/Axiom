import { writable } from "svelte/store";

let socket: WebSocket | undefined = undefined;

const onMessage = new Map<string, (data: any) => void>();

export namespace Websocket {
    export enum State {
        Connecting,
        Connected,
        Disconnected
    }

    export let state = writable(State.Disconnected)

    export function create() {
        createWebsocket()
    }

    export function close() {
        if (socket) {
            Websocket.state.set(Websocket.State.Disconnected)
            socket.onclose = () => {
                socket = undefined
            }
            socket.close()
        }
    }

    export function on(type: string, callback: (data: any) => void) {
        onMessage.set(type, callback);
    }

    export namespace Functions {
        export function ping() {
            socket?.send("ping")
        }

        export function edit(path: string, value: any) {
            console.log(path, value)
            socket?.send(JSON.stringify({
                type: "edit",
                path: path,
                value: value
            }))
        }
    }
}

function createWebsocket() {
    if (socket) Websocket.close()
    socket = new WebSocket("ws://localhost:10464/")
    let liveSocket = socket

    Websocket.state.set(Websocket.State.Connecting)

    socket.onopen = () => {
        console.log("WebSocket connected");
        Websocket.state.set(Websocket.State.Connected)
    };

    socket.onmessage = (event) => {
        let data = JSON.parse(event.data)
        if (onMessage.has(data.type)) {
            onMessage.get(data.type)?.(data)
        }
    };

    socket.onerror = (event) => {
        console.error(event)
    }

    socket.onclose = (event) => {
        Websocket.state.set(Websocket.State.Disconnected)
        if (liveSocket == socket) createWebsocket()
        socket = undefined
    }
}