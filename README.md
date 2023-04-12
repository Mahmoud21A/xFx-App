# xFx-App Protocol
The client opens a connection with the server and *informs* the server whether it wants to *download* or *upload* a file using a *header*.

## List
If the client wants to get the list of files that are shareable by the server, then the header will be as the following:
- **list[one space][--][Line Feed]**

## Download
If the client wants to download a file, then the header will be as the following:
- **download[one space][file name][Line Feed]**

Upon receiving this header, the server searches for the specified file.
- If the file is not found, then the server shall reply with a header as the following:
  - **NOT[one space]FOUND[Line Feed]**
- If the file is found, then the server shall reply
  - with a header as the following:
    - **OK[one space][file size][one space][file last modified time][Line Feed]**
  - If the client has a cached version of the file not more recent than the version on server side
    - If the file sizes are the same, the file on server side is assumed to be unchanged
        the file is not downloaded again.
    - Otherwise, if the file sizes are not equal, the cached if is assumed to be outdated and will be downloaded again.
  - Otherwise, if cached version of the file on client side exists, the file is downloaded following another request by client side with a header like:
  - **download[one space][file name][Line Feed]**
  - The server sends the bytes of the file and the client replies with a header as the following:
    - **FIN[Line Feed]**

## Upload
If the client wants to upload a file, then the header will be as the following:
- **upload[one space][file name][one space][file size][Line Feed]**

After sending the header, the client shall send the bytes of the file
