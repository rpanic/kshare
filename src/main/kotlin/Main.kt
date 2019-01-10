import io.javalin.Javalin
import io.javalin.websocket.WsSession
import java.io.File
import java.nio.file.Files

fun main(args: Array<String>) {
    Main().main();
}

class Main{

    var map = HashMap<String, Editor>()

    fun main() {

        val app = Javalin.create().apply {
            enableCorsForAllOrigins()
            enableStaticFiles("/frontend/monaco")
        }
            .start(8091)
//        app.get("/") {
//            it.result(
//                "Hello world!"
//            )
//        }Abe

        app.get("/:name"){
//            //it.result(it.pathParam("name"))y
//            it.header("location: localhost:8091/index.html?q=${it.pathParam("path")}")
//            it.result("Redirect")
//            it.result("asd")
            var name = it.pathParam("name")
            var path = "src/main/resources/frontend/"
            if(name.contains(".") || name.startsWith("monaco")){
                path += name
            }else{
                path += "index.html"
            }
            if(name.endsWith(".css")) {
                println("serving css")
                it.contentType("text/css")
                it.header("Content-Type", "text/css")
            }
            println("Serving $path")
            try {
                it.html(Files.readAllLines(File(path).toPath()).joinToString("\n").replace("%123%", name))//.joinToString { "\n" })
            }catch (e: Exception){
                e.printStackTrace()
            }
        }

        app.ws("/websocket/:path") { ws ->
            ws.onConnect { session ->
                if(map[session.docId] == null){
                    map[session.docId] = Editor(session.docId)
                }
                session.idleTimeout = 1000000
                map[session.docId]!!.connections.add(Connection(session))
                println("Connected ${session.pathParam("path")}")
            }
            ws.onMessage { session, message ->
                if(!message.startsWith("ping"))
                    println("Received: $message from ${session.id}")

//                var split = message.split(" ")
//
//                when (split[0]) {
//                    "init" -> {
//                        map.put(session.id, Connection(session, Editor() ))
//                    }
//                }
                map[session.docId]!!.request(message,  session)

                //session.remote.sendString("Echo: $message")
            }
            ws.onClose { session, statusCode, reason ->
                println("Closed")
                var sum = map.values.map { it.connections.size }.sum()
                map.values.map { it.connections.removeAll{x -> x.ws.id == session.id} }
                var sumAfter = map.values.map { it.connections.size }.sum()
                sumAfter = sum - sumAfter
                if(sumAfter != 1){
                    println("Removal of connection failed: $sumAfter")
                }
            }
            ws.onError { session, throwable -> println("Errored: ${throwable?.message}") }
        }

        println("Init complete")

    }

    val WsSession.docId: String get() = this.pathParam("path")

}