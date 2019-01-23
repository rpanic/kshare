function loadFiles(){

    let split = location.href.split("/");
    let key = split[split.length - 1];

// **** List Files ****

    $.ajax({
        url: '/listfiles',
        beforeSend: function(request) {
            request.setRequestHeader("key", key);
        },
        type: 'POST',
        dataType: 'json', 
        success: function(data) {
            var response = JSON.parse(data);
            console.log(response);
            useFileJson(response);
        },
        error: function(error) {
            console.log(error);
        }
    });

}

function useFileJson(json){
    console.log(JSON.stringify(json));
    if(json !== undefined){
        console.log(json)
        $("#fileList").html('');
        json.forEach(element => {
            var x = '<div class="ui secondary segment filecont">';
            x += '<div><a href="' + element.networkPath + '" target="blank" class="sfn ls">' + element.name + "</a>";
            x += '<a href="' + element.networkPath + '" class="ui secondary button submit sfn ls" style="float: right; margin-top: -0.6em;">Download</a>';
            x += '<a onClick="copyPath(\'' + url + escape(element.networkPath) + '\');" class="ui secondary button submit sfn ls" style="float: right; margin-top: -0.6em;">Copy Link</button>';
            x+= '';
            x += '</div></div>';
            $("#fileList").append(x);
        });
    }else{
        console.log("Json undefined")
    }
}

function copyPath(path){

    var x = document.createElement('textarea');
    x.value = path;
    document.body.appendChild(x);
    x.select();
    document.execCommand('copy');
    document.body.removeChild(x);

}

function progress(e){
    
    var backgr = "#1b1c1d";
    var color = "#00700b";

    var progress = e.loaded / e.total * 100;

    if(progress === 100){

        setTimeout(() => {

            $("#fakefileselect").css("background", "");
            $("#fakefileselect").css("background-color", backgr);

        }, 1000);

        color = "#005e0a";

    }

    var gradient = "linear-gradient(to right, " + color + " " + progress + "%, " + backgr + " " + progress + "%)";

    $("#fakefileselect").css("background", gradient);

}

function selectFile(event){

    var files = document.querySelector('[type=file]').files;
    if(files.length > 0){

        for (var i = 0; i < files.length; i++) {
            let file = files[i];

            let split = location.href.split("/");
            let key = split[split.length - 1];

// **** Uploading File ****
            
            const formData = new FormData();
            formData.append('file', file);

            $.ajax({
                url: "/uploadfile",
                type: "POST",
                data: formData,
                headers: {
                    "key": key
                }, 
                processData: false,
                contentType: false,
                xhr: function(){
                    var hhh = $.ajaxSettings.xhr();
                    if(hhh.upload){
                        hhh.upload.addEventListener('progress', progress, false);
                    }
                    return hhh;
                }
            })

            // var xhr = new XMLHttpRequest();
            // xhr.upload.addEventListener("progress", (e) => {
            //     console.log("Progress");
            //     console.log(e);
            // }, false);
            // xhr.onreadystatechange = (e) => {
            //     console.log(e);
            //     if (xhr.readyState == 4) {
            //         // progress.className = (xhr.status == 200 ? "success" : "failure");
            //         loadFiles();
            //     }
            // }
            // xhr.open("POST", "/uploadfile", false);
            // xhr.setRequestHeader("X-FILENAME", file.name);
            // xhr.setRequestHeader("key", key);
            // xhr.send(file);

            // const formData = new FormData();
            // formData.append('file', file);
            // fetch('/uploadfile', {
            //     method: 'POST',
            //     body: formData,
            //     headers: {
            //         "key": key
            //     }
            // }).then(response => {
            //     console.log("Fileupload response: " + response);
            //     console.log(response)
            //     if(response.status === 200){
            //         loadFiles();
            //     }
            // });
        }
    }
}