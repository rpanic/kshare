import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.javalin.Context
import io.javalin.Javalin
import io.javalin.staticfiles.Location
import io.javalin.websocket.WsSession
import org.apache.commons.io.FileUtils
import java.io.BufferedInputStream
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.jar.JarFile
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector

fun main(args: Array<String>) {
    Main().main(args)
}

class Main{

    val frontend_version = "1.1.0";

    var map = HashMap<String, Editor>()
    val path = "/filedata/"

    private val writingPath = System.getProperty("user.dir") + "/filedata/"

    private var devPath = ""//""src/main/resources/"

    var numbers = RandomNumbers()

    fun main(args: Array<String>) {

        var port = 80
        var url = "kshare.me";
        var ssl = true;

        for(i in 0 until args.size step 2){
            var key = args[i]
            if(i + 2 > args.size) continue
            if(args[i].startsWith("-p")){
                port = args[i + 1].toIntOrNull() ?: port
            }else if(args[i].startsWith("-u") || args[i].startsWith("-d")){
                url = args[i + 1]
            }else if(args[i].startsWith("-i")) { //-ide

                if(args[i + 1].toBoolean()){

                    devPath = "src/main/resources/"

                }
            }else if(args[i].startsWith("-s")) {   //-ssl

                ssl = args[i + 1].toBoolean()

            }
        }

        var statsFile = userdir() + "/stats.json"

        var stats = Statistics(File(statsFile))
        stats.init()

        println("Starting $url:$port")

        if(url.endsWith("/"))
            url = url.substring(0, url.length - 1)

        extractFrontend("frontend")

        loadEditors()

        numbers.init()

        Thread{
            while(true){
                Thread.sleep(100000L)
                saveEditors()
            }
        }
//        GlobalScope.launch { TODO
//            while(true){
//                delay(100000L)
//                saveEditors()
//            }
//        }

        val app = Javalin.create().apply {

            if(ssl) {
                server {
                    val server = Server()
                    val sslConnector = ServerConnector(server, getSslContextFactory())
                    sslConnector.port = 443
                    val connector = ServerConnector(server)
                    connector.port = 80
                    server.setConnectors(arrayOf<Connector>(sslConnector, connector))
                    server
                }
                before {
                    if (!it.req.isSecure) {
                        var split = it.req.requestURL.split("://")
                        if(split.size >= 2){
                            it.redirect("https://${split.get(1)}")
                        }
                    }
                }
            }

            enableCorsForAllOrigins()
            enableStaticFiles(userdir() + "/frontend/monaco", Location.EXTERNAL)

        }.start(port)

        app.get("/filedata/:name"){

            sendFile(it.pathParam("name"), it)
            stats.addVisit("/filedata")

        }

        app.post("getfile"){

            var key = it.header("key")
            var file = it.header("filename")
            var path = it.header("path")
            if(path != null){
                sendFile(path, it)
            }else if(key != null && file != null)
                sendFile("${key}_{$file}", it)

        }

        app.post("/listfiles"){

            var key = it.header("key")
            var editor = map.values.find { e -> e.name == key }!!

            var moshi = Moshi.Builder().build()

            var files = editor.files()

            var adapter : JsonAdapter<AttachedFile> = moshi.adapter(AttachedFile::class.java)

            var s = files.map { f -> adapter.toJson(f) }.toJsonArray()

            it.json(s)
        }

        app.post("/uploadfile"){

            try {

                var key = it.header("key")
                var editor = map.values.find { e -> e.name == key }!!  //TODO Nullpointer

                if(it.uploadedFiles("file").isEmpty()){
                    println("uploadfile with no files");
                }

                it.uploadedFiles("file").forEach { (_, content, name, _) ->

                    var keyname = key + "_" + name

                    var x = File(writingPath + keyname)
                    var y = x
                    var i = 0
                    println(x.absolutePath)
                    while(y.exists()){
                        y = File("${x.parent}\\${x.nameWithoutExtension}$i.${x.extension}")
                        println(y.absolutePath)
                        i++
                    }

                    if(!x.parentFile.exists()){
                        x.parentFile.mkdirs()
                    }

                    y.createNewFile()
                    FileUtils.copyInputStreamToFile(content, y)

                    editor.addFile(AttachedFile(y.absolutePath, path + keyname, name))
                    stats.addVisit("/uploadFile")

                }
                it.html("success")
            }catch (e: Exception){e.printStackTrace()}
        }

        app.get("/"){
            println("frontpage")
            it.html(File("${devPath}frontend/frontpage.html").readLines()
                .joinToString("\n")
                .replace("%123%", numbers.getNewNumber()))
            stats.addVisit("/")
        }

        app.get("/admin/stats"){

            it.json(stats.toJson())

        }

        app.get("/:name"){ //TODO monaco folder einzeln

            println("name")
            var name = it.pathParam("name")

            var path = "${devPath}frontend/"

            path += if(name.contains(".") || name.startsWith("monaco")){
                name
            }else{
                stats.addVisit("/$name")
                stats.addVisit("/<editor>")
                "index.html"
            }
            if(name.endsWith(".css")) {
                println("serving css")
                it.contentType("text/css")
                it.header("Content-Type", "text/css")
            }
            println("Serving $path")
            try {
                if(name == "favicon.ico"){
                    it.result(File(path).inputStream())
                }else {
                    var s = Files.readAllLines(File(path).toPath()).joinToString("\n")
                    if(path.endsWith(".html")){
                        s = s
                            .replace("%123%", name)
                            .replace("%url%", url)
                            .replace("%ssl%", if(ssl) "wss" else "ws")
                    }
                    it.html(s)
                }
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

            ws.onMessage { session, message ->
                if(!message.startsWith("ping"))
                    println("Received: $message")


                map[session.docId]!!.request(message, session)
            }
            ws.onClose { session, statusCode, reason ->
                println("Closed because: $reason")
                var sum = map.values.map { it.connections.size }.sum()
                map.values.map { it.connections().removeAll{x -> x.ws.id == session.id} }
                var sumAfter = map.values.map { it.connections().size }.sum()
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
            if(s != null){
                if(s!!.startsWith("s")){
                    saveEditors()
                }
            }else{
                break
            }
        }

    }

    private fun sendFile(filename: String, c: Context) = c.also {
        try {

            println("filedata")
            var f = File(writingPath + filename)

            if(!f.exists()) {
                it.result("File ${it.pathParam("name")} not found on our server - go back and try again. \nIf the problem persists contact us")
            }else {
                it.result(f.inputStream())
                it.header("Content-Type", "application/download")
                it.header("Content-Description", "File Transfer")
                it.header("Content-Length", "${f.length()}")
                var name = it.pathParam("name")
                it.header("Content-Disposition", "attachment; filename=${name.substring(name.indexOf('_') + 1)}")
            }

        }catch(e: Throwable){e.printStackTrace()}
    }

    private fun getSslContextFactory(): SslContextFactory {
        val sslContextFactory = SslContextFactory()
        sslContextFactory.keyStorePath = userdir() + "/keystore.jks"
        sslContextFactory.setKeyStorePassword("voyager1")
        return sslContextFactory
    }

    fun extractFrontend(path: String) {

        var root = File(userdir() + "/" + path)
        if(root.isDirectory && root.exists()){

            var versionfile = File(root.path + "/frontend.version")

            if(versionfile.exists()) {
                var version = versionfile.readLines().joinToString()

                if (version.equals(frontend_version)) {
                    return
                } else {
                    println("New version detected: Switching frontend from $version to $frontend_version")
                    root.deleteRecursively()
                }
            }else{
                versionfile.writeText(frontend_version)
            }
        }

        val jarFile = File(javaClass.protectionDomain.codeSource.location.path)

        if (jarFile.isFile) {  // Run with JAR file
            val jar = JarFile(jarFile)
            val entries = jar.entries() //gives ALL entries in jar
            while (entries.hasMoreElements()) {
                val name = entries.nextElement().name
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
//                            extractFrontend(path + "/" + app.name)
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

    private fun userdir() : String = System.getProperty("user.dir")!!

    val WsSession.docId: String get() = this.pathParam("path")

}