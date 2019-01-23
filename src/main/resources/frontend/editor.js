function initLanguageSelect() {

    let languages = monaco.languages.getLanguages();
    languages.forEach(element => {
        let option =  document.createElement("option");
        option.innerHTML = element.aliases[0];
        option.value = element.id;
        $("#syntaxSelect").append(option);
    });
    $("#syntaxSelect").change((e) => {
        let newLanguageId = $("#syntaxSelect option:selected").val();
        monaco.editor.setModelLanguage(window.editor.getModel(), newLanguageId);
    })

}