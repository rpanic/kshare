import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.javalin.Javalin
import io.javalin.websocket.BinaryMessageHandler
import io.javalin.websocket.WsSession
import kotlinx.io.core.String
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.lang.reflect.Type
import java.nio.file.Files
import kotlin.text.StringBuilder

fun main(args: Array<String>) {
    Main().main();
}

class Main{

    var map = HashMap<String, Editor>()
    val path = "/filedata/";

    private val writingPath = System.getProperty("user.dir") + "/src/main/resources/frontend/monaco/filedata/"

    fun main() {

        println(System.getProperty("user.dir"))

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

        app.post("/listfiles"){
try{
            var key = it.header("key")
            var editor = map.values.find { e -> e.name == key }!!;

            var moshi = Moshi.Builder().build()

            var files = editor.files

//            var adapter : JsonAdapter<ArrayList<AttachedFile>> = moshi.adapter(Types.newParameterizedType(files.javaClass, AttachedFile(File(""), "", "").javaClass))
//
//            var json = adapter.toJson(files)
            var adapter : JsonAdapter<AttachedFile> = moshi.adapter(AttachedFile("", "", "").javaClass)

            var s = files.map { adapter.toJson(it) }.map { println(it); it }.joinToString ( ", " )
            s = "[$s]"

            it.json(s)
        }catch (e: Exception){e.printStackTrace()}
        }

        app.post("/uploadfile"){

            try {

                var key = it.header("key");
                var editor = map.values.find { e -> e.name == key }!!

                it.uploadedFiles("file").forEach { (contentType, content, name, extension) ->

                    var keyname = key + "_" + name

                    var x = File(writingPath + keyname)
                    x.createNewFile()
                    FileUtils.copyInputStreamToFile(content, x)

                    editor.files.add(AttachedFile(x.absolutePath, path + keyname, name))

                }
                it.html("success")
            }catch (e: Exception){e.printStackTrace()}
        }

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
                map[session.docId]!!.connections.add(Connection(session, map[session.docId]!!))
                println("Connected ${session.pathParam("path")}")
            }
//            ws.onMessage{ session, msg, offset, length ->
//                println("Bytewise onMessage")
//                var s = String(msg.toByteArray())
//                println("Converted: $s")
//            }
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

                map[session.docId]!!.request(message, session)

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
            ws.onError { session, throwable -> println("Errored: ${throwable?.message}"); throwable?.printStackTrace() }
        }

        println("Init complete")

    }

    val WsSession.docId: String get() = this.pathParam("path")

}