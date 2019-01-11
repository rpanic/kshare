import io.javalin.websocket.WsSession

class Connection(var ws: WsSession, var editor: Editor){

    fun send(s: String){
        ws.remote.sendString(s)
    }

    val path = "/filedata/"
//    private val writingPath = "/resources/frontend/monaco/filedata/"

//    var currentlyUploading = false
//    var writer : FileWriter? = null
//    var attachedFile: AttachedFile? = null
//
//    fun uploadFile(split: List<String>, session: WsSession) {
//
//        var name = "${editor.name}_${split[1]}"  //TODO if file already exists - error back to client
//        var file = File(writingPath + name)
//        file.createNewFile()
//        var networkpath = path + name;
//        this.attachedFile = AttachedFile(file, networkpath, split[1])
//        writer = FileWriter(file)
//        currentlyUploading = true
//        session.remote.sendString("ok")
//
//    }
//
//    fun finishUploading(split: List<String>, session: WsSession){
//
//        writer?.flush()
//        session.remote.sendString("ok fin")
//        editor.files.add(attachedFile!!)
//
//        attachedFile = null
//        writer = null
//        currentlyUploading = false
//
//    }
//
//    fun writeData(s: String){
//        if(currentlyUploading){
//
//            writer?.write(s);
//
//        }
//    }

}