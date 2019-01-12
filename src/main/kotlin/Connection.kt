import io.javalin.websocket.WsSession

data class Connection(var ws: WsSession, var editor: Editor){

    fun send(s: String){
        ws.remote.sendString(s)
    }

}