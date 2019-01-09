function initWs(){

    let split = location.href.split("/");
    var key = split[split.length - 1];
    var ws = new WebSocket("ws://localhost:8091/websocket/" + key);
    window.ws = ws;
    ws.onopen = (event) => {
        ws.send("init " + key);
        console.log("init " + key);
        ws.onmessage = (event) => {
            console.log(event);
            window.editor.setValue(event.data);
            
            ws.onmessage = remoteChanged;
            window.editor.onDidChangeModelContent((event) => {
                onChange(event);
            });
        }
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
    console.log("got one normal " + blocked)
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

function remoteChanged(event){


    let split = event.data.split(" ");
    let v = window.editor.getValue();
    if(split[0] === "ch"){
        
        v = v.slice(0, parseInt(split[1])) + "" + split[2] + v.slice(parseInt(split[1]));
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

    }
   

}