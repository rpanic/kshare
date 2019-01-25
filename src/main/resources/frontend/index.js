function initWs() {

    let split = location.href.split("/");
    let key = split[split.length - 1];
    let ws = new WebSocket("ws://" + location.host + "/websocket/" + key);

    window.ws = ws;
    ws.onclose = (event) => {
        let timeout;
        if (window.timeouts.length < window.timeoutc + 1) {
            timeout = window.timeouts[window.timeouts.length - 1];
        } else {
            timeout = window.timeouts[window.timeoutc];
        }

        console.log("Disconnected - retrying in " + timeout)
        window.timeoutcount++;
        window.timeoutc++;
        setTimeout((event) => { initWs(); }, timeout);
    }
    ws.onopen = (event) => {
        ws.send("init " + key);
        console.log("init " + key);
        ws.onmessage = (event) => {
            window.timeoutc = 0;
            console.log("Connected")

            let pos = window.editor.getPosition()
            window.editor.setValue(event.data);
            window.editor.setPosition(pos)

            ws.onmessage = remoteChanged;
            if (window.timeoutcount == 0) { //Only once because it is the same all the time and otherwise we send 2 or more times the same to closed websockets
                window.editor.onDidChangeModelContent((event) => {
                    onChange(event);
                });
            }
            loadFiles()
        }
    }

    // Interval ping for Websocket to stay open
    if (window.timeoutcount == 0) {
        setInterval(() => {
            if (window.ws.readyState === window.ws.OPEN)
                window.ws.send("ping")
        }, 1000)
    }
}

var blocked = false

function setBlocked(b) {
    blocked = b;
}

var codedUndo = false

function onChange(event) {
    
    console.log(event)

    if ((event.isUndoing === false || codedUndo === false) && event.isFlush === false) {

        if (window.ws.readyState !== window.ws.OPEN) {  //TODO Stehengeblieben beim connect error
            window.editor.getModel().undo();
            codedUndo = true;
            console.log("Not open")
            //TODO Reconnecting Message
            return;
        }

        if (!blocked) {

            event.changes.forEach(element => {

                if (element.rangeLength > 0 && element.text === "") {
                    //Delete
                    window.ws.send("delete " + element.rangeOffset + " " + element.rangeLength);
                } else {
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
        } else {
            setBlocked(false);
        }

    } else if (event.isFlush) {
        console.log("isFlush");
    }

    if (event.isUndoing && codedUndo) {
        codedUndo = false;
    }

}

function remoteChanged(event) {

    if (event.data !== "pong") {
        console.log("remoteChanged " + event.data)
    }
    let split = event.data.split(" ");
    let v = window.editor.getValue();
    if (split[0] === "ch") {

        console.log("debug " + split)
        let newText = event.data.slice(split[0].length + split[1].length + 2);
        console.log("debug2 " + newText)

        let pos = parseInt(split[1]);

        let arr = getLineColumn(pos, v);

        setBlocked(true);
        window.editor.getModel().applyEdits([
            { forceMoveMarkers: false, range: new monaco.Range(arr[0],arr[1],arr[0],arr[1]), text: newText }
        ]);

        // v = v.slice(0, parseInt(split[1])) + "" + newText + v.slice(parseInt(split[1]));
        /*var temp = window.editor.onDidChangeModelContent;
        window.editor.onDidChangeModelContent = () => {
            console.log("got one empty")
            window.editor.onDidChangeModelContent = temp;
        }*/
        // window.editor.setValue(v);
    } else if (split[0] === "del") {

        let offset = parseInt(split[1]);
        let length = parseInt(split[2]);
        
        let from = getLineColumn(offset, v);
        // console.log("From: " + from.toString() + " Offset: " + offset.toString() + " v: " + v.toString());
        let to = getLineColumn(offset + length, v);
        // console.log("TO: " + to)

        console.log(from[0] + " " + from[1] + " " + to[0] + " " + to[1]);
        
        // window.editor.pushUndoStop();
        setBlocked(true);
        window.editor.getModel().applyEdits([
            { forceMoveMarkers: false, range: new monaco.Range(from[0],from[1],to[0],to[1]), text: null }
        ]);
        // window.editor.pushUndoStop();
        
        console.log("del executed");

        // window.editor.setValue(v);
    } else if (split[0] === "failed") {
        codedUndo = true;
        window.editor.getModel().undo();
    } else if (split[0] === "rlfiles") {
        loadFiles()
    }
}

function getLineColumn(pos, str){
    let line = 1;
    let column = 0;
    let count = 0;

    for (var i = 0; i < str.length && i < pos; i++) {
        let char = str.charAt(i);
        if(char === '\n'){
            line++;
            column = 0;
        }else{
            column++;
        }
        count ++;
    }

    if(count != pos){
        console.log("Something wrong in algorithm");
    }
    
    return [line, column + 1];
}



$(window).ready(() => {
    $("[type=file]").hide();

    $('.ui.dropdown')
        .dropdown();
})

function simulateClick() {
    $("[type=file]").click()
}

function fileselectchanged() {
    var f = document.querySelector("[type=file]").files
    console.log("fileselectchanged " + f.length)
    if (f.length > 0) {
        var text = "";
        if (f.length > 1) {
            text = f.length + " files selected";
        } else {
            text = f[0].name;
        }
        $("#fakefileselect").html(text)
    }
}