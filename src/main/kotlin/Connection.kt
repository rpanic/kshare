import io.javalin.websocket.WsSession

class Connection(var ws: WsSession){

    fun send(s: String){
        ws.remote.sendString(s)
    }

}