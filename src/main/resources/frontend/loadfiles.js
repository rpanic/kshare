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

const url = 'process.php';
const form = document.querySelector('form');
form.addEventListener('submit', selectFile)

function selectFile(event){
    const file = document.querySelector('[type=file]').files[0];
    const formData = new FormData();
    formData.append('file', file);
    fetch(url, {
        url: '/uploadFile?k=' + key,
        method: 'POST',
        body: formData
    }).then(response => {
        console.log(response);
    });
}