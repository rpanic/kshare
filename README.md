# kShare

#### kShare is a real-time code and file sharing platform

- Backend written in Kotlin with [javalin](javalin.io)
- built-in [monaco](https://microsoft.github.io/monaco-editor/) editor
- easy-to-use file upload [api](https://github.com/rpanic/kshare/wiki/File-Upload-API-Documentation)

A running instance of kShare can be used at [rpanic.com](https://rpanic.com)

[![Build Status](https://drone.rpanic.com/api/badges/rpanic/kshare/status.svg)](https://drone.rpanic.com/rpanic/kshare)

#### Editors instances

Every new editor and file-storing instance has a unique key. \
The key of a instance displayed in the url like: `kshare.me/key` 

You can also generate a random key by going to the index.html (`f.e. "kshare.me/"`)

[Test it yourself](rpanic.com)

#### Install it yourself

You can either download one of our \
[releases](https://github.com/rpanic/kshare/releases) \
or compile it yourself with gradle.

Our releases already include the monaco and frontend resources

If you want to compile it yourself you will have to download the [monaco editor](https://microsoft.github.io/monaco-editor/) and [Semantic-UI](https://semantic-ui.com/) manually and put the extracted files into `/resources/frontend/monaco` folder.

**Execute** the app with

`java -jar kshare.jar`
    
After executing, kshare will extract the necessary static files to the folder `frontend/`

You will have to pass your url as an argument, for example:\
`... -url myurl.com`
    
The default port is 80, but can be changed by the port argument\
`... -port 80`

SSH is enabled by default, disable it with\
`... -ssh false`

When executing from an IDE append `-ide true`

#### File upload API

The File upload API is designed to be minimalistic and easy to use. \
Therefore there are only 3 endpoints

- `listFiles: Lists all files related to a given key`
- `uploadFile: Uploads one or multiple files`
- `fileData: retrieves a file`

**listfiles**

`POST: kshare.me/listFiles`

Header Parameter: 
- key: Specifies the key to which the file was saved. \
Note: The key has be specified int he HTTP Header!

Sample Result: 
```
[{
    "name":"test.txt",
    "networkPath":"/filedata/testkey_test.txt"
}]
```

**uploadFile**

`POST: kshare.me/uploadFile`

Parameters: 
- key (header): Specifies the key to which the file should be saved.
- file (body): The data of the file being uploaded 

Result: \
`ok:` If the upload was successful \
`failed:` There were problems or your request was wrong

**getFile**

`POST: kshare.me/getfile`

Parameters:
- key (header): Specifies the key to which the file was saved.
- filename (header): The name of the file saved

Result: \
A data stream of the files data

