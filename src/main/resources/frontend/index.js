function initWs(){

    let split = location.href.split("/");
    let key = split[split.length - 1];
    let ws = new WebSocket("ws://localhost:8091/websocket/" + key);
    //ws.timeoutcount = window.timeoutcount
    window.ws = ws;
    ws.onclose = (event) => {
        let timeout = window.timeouts[window.timeoutc];
        console.log("Disconnected - retrying in " + timeout)
        window.timeoutcount++;
        window.timeoutc++;
        setTimeout((event) => {initWs();}, timeout);
    }
    ws.onopen = (event) => {
        ws.send("init " + key);
        console.log("init " + key);
        ws.onmessage = (event) => {
            window.timeoutc = 0;
            console.log("Connected")
            //setBlocked(true)
            window.editor.setValue(event.data);
            
            ws.onmessage = remoteChanged;
            if(window.timeoutcount == 0){ //Only once because it is the same all the time and otherwise we send 2 or more times the same to closed websockets
                window.editor.onDidChangeModelContent((event) => {
                    onChange(event);
                });
            }
        }
        //ws.o
    }

    if(window.timeoutcount == 0){
        setInterval(() => {
            if(window.ws.readyState === window.ws.OPEN)
                window.ws.send("ping")
        }, 1000)
    }
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

    if(event.isUndoing === false && event.isFlush === false){

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

                    let elementtext = element.text;
                    /*if(element.rangeLength !== elementtext.length){
                        elementtext = elementtext.slice(element.rangeLength);
                        v = window.editor.getValue();
                        v = v.slice(0, element.rangeOffset + element.rangeLength) + v.slice(element.rangeOffset + element.text.length, v.length);
                        setBlocked(true);
                        window.editor.setValue(v);
                    }*/ //TODO Autocomplete fixen - zurzeit deaktiviert

                    window.ws.send("insert " + element.rangeOffset + " " + elementtext);
                }

            });
        }else{
            setBlocked(false);
        }

    }else if(event.isFlush){
        console.log("isFlush");
    }

}

function remoteChanged(event){

    if(event.data !== "pong"){
        console.log("remoteChanged " + event.data)
    }
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