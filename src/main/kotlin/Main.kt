import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.javalin.Javalin
import io.javalin.websocket.WsSession
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import java.io.BufferedInputStream
import java.io.File
import java.net.URISyntaxException
import java.nio.file.*
import java.util.*
import java.util.jar.JarFile

fun main(args: Array<String>) {
    Main().main(args)
}

class Main{

    var map = HashMap<String, Editor>()
    val path = "/filedata/"

    private val writingPath = System.getProperty("user.dir") + "/filedata/"

    private val devPath = "src/main/resources/";

    var numbers = RandomNumbers()

    fun main(args: Array<String>) {

        var port = 80;

        if(args.size > 1){
            if(args[0].startsWith("-p")){
                port = args[1].toIntOrNull() ?: port
            }
        }

        extractResource("frontend")

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

        }.start(port)
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

            var files = editor.files

//            var adapter : JsonAdapter<ArrayList<AttachedFile>> = moshi.adapter(Types.newParameterizedType(files.javaClass, AttachedFile(File(""), "", "").javaClass))
//
//            var json = adapter.toJson(files)
            var adapter : JsonAdapter<AttachedFile> = moshi.adapter(AttachedFile::class.java)

//            var s = files.map { adapter.toJson(it) }.map { println(it); it }.joinToString ( ", " )
//            s = "[$s]"
            var s = files.map { adapter.toJson(it) }.toJsonArray()

            it.json(s)
        }

        app.post("/uploadfile"){

            try {

                var key = it.header("key")
                var editor = map.values.find { e -> e.name == key }!!  //TODO Nullpointer

                it.uploadedFiles("file").forEach { (_, content, name, _) ->

                    var keyname = key + "_" + name

                    var x = File(writingPath + keyname)
                    var y = x;
                    var i = 0;
                    println(x.absolutePath)
                    while(y.exists()){
                        y = File("${x.parent}\\${x.nameWithoutExtension}$i.${x.extension}");
                        println(y.absolutePath)
                        i++;
                    }

                    if(!x.parentFile.exists()){
                        x.parentFile.mkdirs()
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
            it.html(File("${devPath}frontend/frontpage.html").readLines().joinToString("\n").replace("%123%", numbers.getNewNumber()))
        }

        app.get("/:name"){ //TODO monaco folder einzeln

            println("name")
            var name = it.pathParam("name")

            var path = "${devPath}frontend/"

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
                var s = Files.readAllLines(File(path).toPath()).joinToString("\n").replace("%123%", name)  //TODO Replace nur bei html oder so
                it.html(s)//.joinToString { "\n" })
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

    fun extractResource(path: String) {

        var root = File(userdir() + "/frontend")
        if(root.isDirectory && root.exists()){
            return;
        }

        val jarFile = File(javaClass.protectionDomain.codeSource.location.path)

        if (jarFile.isFile()) {  // Run with JAR file
            val jar = JarFile(jarFile)
            val entries = jar.entries() //gives ALL entries in jar
            while (entries.hasMoreElements()) {
                val name = entries.nextElement().getName()
                if (name.startsWith("$path/")) { //filter according to the path
                    var split = name.split("/")
                    if(split[split.lastIndex].contains(".")){
                        var inst = Main::class.java.getResourceAsStream(name)
                        println("extracting $name...")
                        var f =  File(userdir() + "/" + name)
                        if(!f.parentFile.exists())
                            f.parentFile.mkdirs()
                        f.createNewFile()
                        f.writeBytes(BufferedInputStream(inst).readBytes())
                    }
                }
            }
            jar.close()
        } //else { // Run with IDE
//            val url = Main::class.java.getResource("/$path")
//            if (url != null) {
//                try {
//                    val apps = File(url.toURI())
//                    for (app in apps.listFiles()) {
//                        System.out.println(app)
//                        if (app.isDirectory) {
//                            extractResource(path + "/" + app.name)
//                        } else {
//                            if(!app.parentFile.exists())
//                                app.parentFile.mkdir()
//
//                            File(app.path).writeBytes(app.readBytes())
//                        }
//                    }
//                } catch (ex: URISyntaxException) {
//                    // never happens
//                }
//
//            }
//        }
    }

    val savePath = userdir() + "/save.csv"

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
                if(!fileDirectory.exists())
                    fileDirectory.mkdirs()
                fileDirectory.listFiles { f -> f.name.startsWith(it.name) }
                        .map { f -> AttachedFile(f.absolutePath, path + f.name, f.name.substring(it.name.length + 1)) }
                        .forEach{ f -> it.files().add(f)}

            }
        }
    }

    fun Iterable<String>.toJsonArray() =
        "[${this.joinToString(", ")}]"

    fun userdir() = System.getProperty("user.dir")

    val WsSession.docId: String get() = this.pathParam("path")

}