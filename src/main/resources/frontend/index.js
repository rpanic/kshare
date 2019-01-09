function initWs(){

    let split = location.href.split("/");
    var key = split[split.length - 1];
    var ws = new WebSocket("ws://localhost:8091/websocket/" + key);
    //ws.timeoutcount = window.timeoutcount
    window.ws = ws;
    ws.onclose = (event) => {
        let timeout = window.timeouts[window.timeoutcount];
        console.log("Disconnected - retrying in " + timeout)
        window.timeoutcount++;
        setTimeout((event) => {initWs();}, timeout);
    }
    ws.onopen = (event) => {
        ws.send("init " + key);
        console.log("init " + key);
        ws.onmessage = (event) => {
            console.log(event);
            setBlocked(true)
            window.editor.setValue(event.data);
            setBlocked(false)
            
            ws.onmessage = remoteChanged;
            if(window.timeoutcount == 0){ //Only once because it is the same all the time and otherwise we send 2 or more times the same to closed websockets
                window.editor.onDidChangeModelContent((event) => {
                    onChange(event);
                });
            }
        }
        ws.o
    }

    setInterval(() => {
        if(window.ws.readyState === window.ws.OPEN)
            window.ws.send("ping")
    }, 1000)
}

function executeIfFree(f){
    if(!blocked){
        f();
    }else{
        fun = f;
    }
}

var fun = () => {};

var blocked = false

function setBlocked(b){
    blocked = b;
    if(blocked && !b){
        fun();
    }
}

function onChange(event){
    //console.log(event)
    console.log(event)
    console.log("got one normal " + blocked)

    if(event.isUndoing === false){

        if(window.ws.readyState !== window.ws.OPEN){  //TODO Stehengeblieben beim connect error
            window.editor.getModel().undo();
            //TODO Reconnecting Message
            return;
        }

        if(!blocked){

            event.changes.forEach(element => {
                
                if(element.rangeLength > 0 && element.text === ""){
                    //Delete
                    window.ws.send("delete " + element.rangeOffset + " " + element.rangeLength);
                }else{
                    //Insert
                    window.ws.send("insert " + element.rangeOffset + " " + element.text);
                }

            });
        }else{
            setBlocked(false);
        }

    }

}

function remoteChanged(event){

    console.log("remoteChanged " + event.data)
    let split = event.data.split(" ");
    let v = window.editor.getValue();
    if(split[0] === "ch"){
        
        console.log("debug " + split)
        let newText = event.data.slice(split[0].length + split[1].length + 2);
        console.log("debug2 " + newText)
        v = v.slice(0, parseInt(split[1])) + "" + newText + v.slice(parseInt(split[1]));
        /*var temp = window.editor.onDidChangeModelContent;
        window.editor.onDidChangeModelContent = () => {
            console.log("got one empty")
            window.editor.onDidChangeModelContent = temp;
        }*/
        setBlocked(true);
        window.editor.setValue(v);
    }else if(split[0] === "del"){

        let offset = parseInt(split[1]);
        let length = parseInt(split[2]);
        v = v.slice(0, offset) + v.slice(offset + length, v.length)
        setBlocked(true)
        window.editor.setValue(v);
    }
   

}