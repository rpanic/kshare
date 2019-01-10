function loadFiles(){

    let split = location.href.split("/");
    let key = split[split.length - 1];
    $.get("/listfiles?k=" + key, (data) => {
        $("#fileList").html('');
        data.forEach(element => {
            var x = '<div class="ui secondary segment filecont">';
            x += '<a href="' + element.href + '">' + element.name + '</a>';
            x += '</div>';
            $("#fileList").append(x);
        });
    })

}

function selectFile(event){

    var files = document.querySelector('[type=file]').files;
    if(files.length > 0){

        var file = files[0];

        let split = location.href.split("/");
        let key = split[split.length - 1];

        const formData = new FormData();
        formData.append('file', file);
        //formData.append('key', key);
        fetch('/uploadfile', {
            method: 'POST',
            body: formData,
            headers: {
            //    "Content-Type": "multipart/form-data"
                "key": key
            }
        }).then(response => {
            console.log("Fileupload response: " + response);
            console.log(response)
        });
    }
}