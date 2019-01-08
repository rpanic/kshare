
let split = location.pathname.split(";");
var key = split[split.length];
var ws = new WebSocket("ws://localhost:8091/websocket/" + key);
ws.onopen = (event) => {
    ws.send("init " + key);
    console.log("init " + key);
    ws.onmessage = (event) => {
        console.log("recieved " + event.data);
    }
}