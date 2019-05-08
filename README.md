# webserver

Usage:

Preferably do a Gradle import into your IDE of choice (like IntelliJ).
Build with Gradle and run the Spring Boot app (org.mbachran.server.ServerApplication).
See it working by accessing (with default settings):
- http://localhost:7070/index.html to see it load a html page with a PNG and the JQuery js file
- http://localhost:7070/Dossier.pdf to see it load a binary actually a PDF
- http://localhost:7070/foobar.json to load a plain text file
- Run org.mbachran.server.custom.ServerTests

Code was written with Java 11.

----------------------------------------------

This is sample code for a simple java based blocking I/O (NIO) http server.

This is a coding practise using plain Java with
    - Gradle for build
    - Spring Boot to benefit from dependency injection only (embedded web server is disabled)
    - Apache commons-lang and commons-io in rare cases for simplification of code
    - SLF4J for logging
    - Findbugs as implementation of jsr305 for annotations around null behavior
    - JSON for json (un)marshalling for the JSON updates as provided in the default SPI implementation as found in the storage package
    - Testing benefits from Spring Boot as well (see ServerTests class)

Supported:
    - HttpVersion - 1.1
    - Methods - GET, PUT, POST, DELETE, HEAD, OPTIONS
    - RequestLine - unlimited length

Transfer encoding parsers:
    - identity, chunked (responses will use no transfer encoding)

Header behavior:
    - Content length:
        - used for body termination in case of transfer encoding 'identity'

    - Connection:
        - close is responded in case the server decides to close the socket.
        - keep alive is interpreted and session is kept alive

    - Keep alive:
        - timeout can temporarily change the socket read timeout, max is counted as long as it is continuously send (gaps will reset)

    - Media type:
        - different content type handlers can be implemented each for a set of media types;
        - media type is not used to detect body end, the parser needs to be enhanced for that
        - especially multipart types are not supported. This also means boundary is ignored.

    - Header value reading is based on UTF-8 and no further parsing of the values regarding the parameters is done.

Default behavior:
    - simple file persistence
    - POST allows attribute based updates within JSON files
    - no protection where file access takes place
    - file root location if derived from classpath based resource location!

Extensibility
    - via Spring wiring implementations can be exchanged
    - via configuration custom NamedHandlers can be configured in
    - dispatch chain allows provisioning of further handlers for HTTP versions, content types and methods
    - BodyParsers can be provided to support further transfer encoding and media type specific behavior

SPI
    - for behavior implementors there is no servlet or HttpRequest, HttpResponse style like SPI as known from Tomcat and others
    - the SPI is rather small and based upon NamedHandler, Request and Response classes and handlers are configured per HTTP method.

TODOs
    - depending on the desired extension points and SPIs the code needs to be validated and made mor SOLID.
    - there has been no check yet regarding full HTTP compatibility
    - some packages depend on each other on implementation level (e.g. request.impl and parser.impl are coupled regarding the default implementations)
    - packages 'response' and 'dispatch.impl' are immature