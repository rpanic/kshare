import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.javalin.Context
import io.javalin.Javalin
import io.javalin.websocket.WsSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files

fun main(args: Array<String>) {
    Main().main()
}

class Main{

    var map = HashMap<String, Editor>()
    val path = "/filedata/"

    private val writingPath = System.getProperty("user.dir") + "/src/main/resources/filedata/"

    var numbers = RandomNumbers()

    fun main() {

        loadEditors()

        numbers.init()

        GlobalScope.launch {
            while(true){
                delay(100000L)
                saveEditors()
            }
        }

        println(System.getProperty("user.dir"))

        val app = Javalin.create().apply {
            enableCorsForAllOrigins()
            enableStaticFiles("/frontend/monaco")
        }.start(80/*91*/)
//        app.get("/") {
//            it.result(
//                "Hello world!"
//            )
//        }Aber

        app.get("/filedata/:name"){

//            if(name.startsWith("filedata")){
//                println("FIledata $name")
//            }
            try {
                println("filedata")
                var f = File(writingPath + it.pathParam("name"))
                println(f.absolutePath)
                if(!f.exists()) {
                    it.result("File ${it.pathParam("name")} not found on our server - go back and try again. \nIf the problem persists contact us")
                }else {
                    it.result(f.inputStream())
                    it.header("Content-Type", "application/download")
                    it.header("Content-Description", "File Transfer")
                    it.header("Content-Length", "${f.totalSpace}")
                    var name = it.pathParam("name")
                    it.header("Content-Disposition", "attachment; filename=${name.substring(name.indexOf('_') + 1)}")
                }

            }catch(e: Throwable){e.printStackTrace()}
        }

        app.post("/listfiles"){

            var key = it.header("key")
            var editor = map.values.find { e -> e.name == key }!!

            var moshi = Moshi.Builder().build()

            var files = editor.files()

//            var adapter : JsonAdapter<ArrayList<AttachedFile>> = moshi.adapter(Types.newParameterizedType(files.javaClass, AttachedFile(File(""), "", "").javaClass))
//
//            var json = adapter.toJson(files)
            var adapter : JsonAdapter<AttachedFile> = moshi.adapter(AttachedFile("", "", "").javaClass)

//            var s = files.map { adapter.toJson(it) }.map { println(it); it }.joinToString ( ", " )
//            s = "[$s]"
            var s = files.map { adapter.toJson(it) }.toJsonArray()

            it.json(s)
        }

        app.post("/uploadfile"){

            try {

                var key = it.header("key")
                var editor = map.values.find { e -> e.name == key }!!

                it.uploadedFiles("file").forEach { (contentType, content, name, extension) ->

                    var keyname = key + "_" + name

                    var x = File(writingPath + keyname)
                    var y = x;
                    var i = 0;
                    println(x.absolutePath)
                    while(y.exists()){
                        y = File(x.parent + "\\" + x.nameWithoutExtension + i + "." + x.extension);
                        println(y.absolutePath)
                        i++;
                    }

                    y.createNewFile()
                    FileUtils.copyInputStreamToFile(content, y)

                    editor.addFile(AttachedFile(y.absolutePath, path + keyname, name))

                }
                it.html("success")
            }catch (e: Exception){e.printStackTrace()}
        }

        app.get("/"){
            println("frontpage")
            it.html(File("src/main/resources/frontend/frontpage.html").readLines().joinToString("\n").replace("%123%", numbers.getNewNumber()))
        }


//        app.wsLogger{x ->
//            x.onMessage { session, msg -> println("msg $msg") }
//        }

        app.get("/:name"){
//            //it.result(it.pathParam("name"))y
//            it.header("location: localhost:8091/index.html?q=${it.pathParam("path")}")
//            it.result("Redirect")
//            it.result("asd")
            println("name")
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
                map[session.docId]!!.connections().add(Connection(session, map[session.docId]!!))
                println("Connected ${session.pathParam("path")}")
            }
//            ws.onMessage{ session, msg, offset, length ->
//                println("Bytewise onMessage")
//                var s = String(msg.toByteArray())
//                println("Converted: $s")
//            }
            ws.onMessage { session, message ->
                if(!message.startsWith("ping"))
                    println("Received: $message")

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
                println("Closed because: $reason")
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

        while(true){
            var s = readLine()
            if(s!!.startsWith("s")){
                saveEditors()
            }
        }

    }

    val savePath = System.getProperty("user.dir") + "/save.csv"

    fun saveEditors(){

//        var text = map.values.map { it.name + ";" + it.text }.joinToString("\n")
//        File(savePath).writeText(text)
        var adapter = Moshi.Builder().build().adapter<Editor>(Editor::class.java)
        var arr = map.values.map { adapter.toJson(it) }.map { println(it); it }.toJsonArray()

        File(savePath).writeText(arr)

    }

    fun loadEditors(){

        map.clear()

        var adapter = Moshi.Builder().build().adapter<List<Editor>>(Types.newParameterizedType(List::class.java, Editor::class.java))

        var file = File(savePath)

        if(file.exists()) {

            var s = file.readLines().joinToString()

            var arr = adapter.fromJson(s)
            arr!!.forEach {
                map[it.name] = it

                var fileDirectory = File(writingPath)
                fileDirectory.listFiles { f -> f.name.startsWith(it.name) }
                        .map { f -> AttachedFile(f.absolutePath, path + f.name, f.name.substring(it.name.length + 1)) }
                        .forEach{ f -> it.files().add(f)}

            }
        }

//        lines.forEach {
//            var split = it.split(";")
//            map[split[0]] = Editor(split[0])
//            map[split[0]]!!.text = split[1]
//        }

    }

    fun Iterable<String>.toJsonArray() =
        "[${this.joinToString(", ")}]"


    val WsSession.docId: String get() = this.pathParam("path")

}