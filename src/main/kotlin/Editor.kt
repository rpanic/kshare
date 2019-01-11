import io.javalin.websocket.WsSession

class Editor (var name: String) {

    @Transient var connections = ArrayList<Connection>()
    var text = "Write something here"
    @Transient var files = ArrayList<AttachedFile>()
    var lastUsed = 0L
    @Transient var rlFileList = ArrayList<String>()

    constructor(name: String, text: String) : this(name){
        this.text = text
    }

    fun connections() : ArrayList<Connection>{
        if(connections == null){
            connections = ArrayList()
        }
        return connections
    }

    fun files() : ArrayList<AttachedFile>{
        if(files == null){
            files = ArrayList()
        }
        return files
    }

    fun rlFileList() : ArrayList<String>{
        if(rlFileList == null){
            rlFileList = ArrayList()
        }
        return rlFileList
    }

    fun addFile(file: AttachedFile){
        files().add(file)
        rlFileList.addAll(connections().filter { it.ws.isOpen }.map { it.ws.id })
    }

    fun request(s: String, session: WsSession){

        lastUsed = System.currentTimeMillis()

        var split = s.split(" ")

        when (split[0]){

            "insert" -> {
                insert(split, session, s)
            }
            "delete" -> {
                delete(split, session)
            }
            "init" -> {
                init(split, session)
            }
            "ping" -> {
                if(rlFileList().contains(session.id)){
                    session.remote.sendString("rlfiles")
                    rlFileList().remove(session.id)
                }else
                    session.remote.sendString("pong")
            }
//            "uploadFile" -> {
//                getConnection(session.id)!!.uploadFile(split, session)
//            }
//            "fin" -> {
//                getConnection(session.id)!!.finishUploading(split, session)
//            }

        }

    }

    fun getConnection(sessionid: String) : Connection?{
        return connections.find { it.ws.id == sessionid }
    }

    private fun delete(split: List<String>, session: WsSession) {

        var offset = split[1].toInt()
        var length = split[2].toInt()

        text = text.substring(0, offset) + text.substring(offset + length, text.length)

        advertiseToAllExcept(session, "del ${split[1]} ${split[2]}")
        session.remote.sendString("ok")

    }

    private fun init(split: List<String>, session: WsSession) {

        session.remote.sendString(text)

    }

    private fun insert(split: List<String>, session: WsSession, s: String) {

        var offset = split[1].toInt()

        var newtext = s.substring(split[0].length + split[1].length + 2)

        if(offset > text.length){
            offset = text.length - 1
        }
        text = StringBuilder(text).insert(offset, newtext).toString()

        advertiseToAllExcept(session, "ch ${split[1]} $newtext")

        session.remote.sendString("ok")

        //session.remote.sendString("true")

    }

    private fun advertiseToAllExcept(session: WsSession, s: String){
        connections.map { it.ws }
            .filter { it.id != session.id }
            .filter { it.isOpen }
            .forEach { it.remote.sendString(s); println("Sent $s to ${it.id}") }
    }

}
