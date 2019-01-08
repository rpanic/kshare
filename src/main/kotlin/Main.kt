import io.javalin.Javalin
import io.javalin.websocket.WsSession

fun main(args: Array<String>) {
    Main().main();
}

class Main{

    var map = HashMap<String, Editor>()

    fun main() {

        val app = Javalin.create().apply {
            enableCorsForAllOrigins()
//            enableStaticFiles("/public")
        }
            .start(8091)
//        app.get("/") {
//            it.result(
//                "Hello world!"
//            )
//        }
//
//        app.get("/:name"){
//            it.result(it.pathParam("name"))
//        }

        app.ws("/websocket/:path") { ws ->
            ws.onConnect { session ->
                if(map[session.docId] == null){
                    map[session.docId] = Editor(session.docId)
                }
                map[session.docId]!!.connections.add(Connection(session))
                println("Connected ${session.pathParam("path")}")
            }
            ws.onMessage { session, message ->
                println("Received: $message")

//                var split = message.split(" ")
//
//                when (split[0]) {
//                    "init" -> {
//                        map.put(session.id, Connection(session, Editor() ))
//                    }
//                }

                session.remote.sendString("Echo: $message")
            }
            ws.onClose { session, statusCode, reason -> println("Closed") }
            ws.onError { session, throwable -> println("Errored") }
        }

        println("Init complete")

    }

    val WsSession.docId: String get() = this.pathParam("path")

}