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
            x += '<a href="' + element.networkPath + '" target="blank">' + element.name + '</a>';
            x += '</div>';
            $("#fileList").append(x);
        });
    }else{
        console.log("Json undefined")
    }
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
            fetch('/uploadfile', {
                method: 'POST',
                body: formData,
                headers: {
                    "key": key
                }
            }).then(response => {
                console.log("Fileupload response: " + response);
                console.log(response)
                if(response.status === 200){
                    loadFiles();
                }
            });
        }
    }
}