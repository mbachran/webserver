package org.mbachran.server.custom;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Testing with buffer size 1 to ensure buffer can end at any position in the stream without breaking the parser.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"application.config.custom-server.port:7071", "application.config.custom-server.connection.read-buffer-size:1"})
public class ServerTests
{
    private static final Logger LOG = LoggerFactory.getLogger(ServerTests.class);

    @Autowired
    private ServerConfig serverConfig;

    @Test
    public void testGetByRequestLineOnly() throws IOException
    {
        final String expectedResponse = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n";
        final String readData = openWriteReadClose("GET /path/not/found/ HTTP/1.1\r\n\r\n");
        Assert.assertEquals(expectedResponse, readData);
    }

    @Test
    public void testGetWithSingleLineHeaders() throws IOException
    {
        final String expectedResponse = "HTTP/1.1 200 OK\r\nContent-Length: 20\r\n\r\n{\r\n  \"foo\": \"bar\"\r\n}";
        final String data = "GET /foobar.json HTTP/1.1\r\nCache-Control: no-cache\r\n\r\n";
        final String readData = openWriteReadClose(data);
        Assert.assertEquals(expectedResponse, readData);
    }

    @Test
    public void testGetWithConnectionClose() throws IOException
    {
        final String expectedResponse = "HTTP/1.1 200 OK\r\nConnection: close\r\nContent-Length: 20\r\n\r\n{\r\n  \"foo\": \"bar\"\r\n}";
        final String data = "GET /foobar.json HTTP/1.1\r\nConnection: close\r\n\r\n";
        final String readData = openWriteReadClose(data);
        Assert.assertEquals(expectedResponse, readData);
    }

    @Test
    public void testGetWithMultiLineHeaders() throws IOException
    {
        final String expectedResponse = "HTTP/1.1 200 OK\r\nContent-Length: 20\r\n\r\n{\r\n  \"foo\": \"bar\"\r\n}";
        final String data = "GET /foobar.json HTTP/1.1\r\nmy-custom-multi-line-header: first\r\n\tsecond\r\n third\r\n\r\n";
        final String readData = openWriteReadClose(data);
        Assert.assertEquals(expectedResponse, readData);
    }

    @Test
    public void testHeadRequest() throws IOException
    {
        final String expectedResponse = "HTTP/1.1 204 No Content\r\n\r\n";
        final String data = "HEAD /foobar.json HTTP/1.1\r\n\r\n";
        final String readData = openWriteReadClose(data);
        Assert.assertEquals(expectedResponse, readData);
    }

    @Test
    public void testOptionsRequest() throws IOException
    {
        final String expectedResponse = "HTTP/1.1 204 No Content\r\nAllow: DELETE, GET, HEAD, OPTIONS, POST, PUT\r\n\r\n";
        final String data = "OPTIONS /foobar.json HTTP/1.1\r\n\r\n";
        final String readData = openWriteReadClose(data);
        Assert.assertEquals(expectedResponse, readData);
    }

    @Test
    public void testPutChunkedRequest() throws IOException
    {
        final String expectedResponse = "HTTP/1.1 201 Created\r\nContent-Length: 0\r\n\r\n";
        final String chunk1 = "{\"chunk1\":\"first\",";
        final String chunk2 = "\"chunk2\":\"second\"}";
        final int chunk1Length = chunk1.getBytes(UTF_8).length;
        final int chunk2Length = chunk2.getBytes(UTF_8).length;
        final String data = "PUT /chunk.json HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n" +
                Integer.toHexString(chunk1Length) + "\r\n" +
                chunk1 + "\r\n" +
                Integer.toHexString(chunk2Length) + ";extension\r\n" +
                chunk2 + "\r\n0\r\ntrailer\r\n";

        final String readData = openWriteReadClose(data);
        Assert.assertEquals(expectedResponse, readData);
    }

    /**
     * Ensuring sequence create, update, get, delete works for the JSON scenario.
     * Covering creation via POST as well for both JSON and text.
     * Used files a deleted upfront to ensure the test is not flacky (while not checking the response as it varies depending on the starting state).
     * <p>
     * This test is using the same connection via keep-alive first by sending the headers in various variations
     * then by omitting it relying on the default behavior. Server behavior is not asserted though.
     * <p>
     * This test is probably a little lazy. As headers are in a HashMap (implementation detail) and order is not guaranteed (message aka API
     * contract) the asserts might be flacky when the test compares multiple headers.
     *
     * @throws IOException in case of any failure
     */
    @Test
    public void testPutPostGetDeleteJsonKeepAlive() throws IOException
    {
        SocketChannel socket = openSocket();

        // cleanup via DELETE to ensure starting point
        writeRead("DELETE /putty.json HTTP/1.1\r\nConnection: keep-alive\r\n\r\n", socket);
        writeRead("DELETE /post_it.json HTTP/1.1\r\nKeep-Alive: timeout=5\r\n\r\n", socket);
        writeRead("DELETE /post_it.txt HTTP/1.1\r\nConnection: keep-alive\r\nKeep-Alive: timeout=5\r\n\r\n", socket);
        writeRead("DELETE /post_it.json HTTP/1.1\r\nConnection: keep-alive\r\nKeep-Alive: max=100\r\n\r\n", socket);

        // create JSON via PUT
        final String expectedPutResponse = "HTTP/1.1 201 Created\r\nConnection: keep-alive\r\nContent-Length: 0\r\n\r\n";
        final String putBody = "{\"created-by\":\"put\"}";
        final String putData = "PUT /putty.json HTTP/1.1\r\nConnection: keep-alive\r\nKeep-Alive: timeout=5, max=100\r\nContent-Length: " + putBody.getBytes(
                UTF_8).length + "\r\n\r\n" + putBody;

        final String readPutResponse = writeRead(putData, socket);
        Assert.assertEquals(expectedPutResponse, readPutResponse);

        // update JSON via POST
        final String expectedPostBody = "{\"created-by\":\"put\",\"modified-by\":\"post\"}";
        final String expectedPostResponse = "HTTP/1.1 200 OK\r\nContent-Length: " + expectedPostBody.getBytes(
                UTF_8).length + "\r\n\r\n" + expectedPostBody;
        final String postBody = "{\r\n  \"modified-by\": \"post\"\r\n}";
        final String postData = "POST /putty.json HTTP/1.1\r\nContent-Length: " + postBody.getBytes(UTF_8).length + "\r\n\r\n" + postBody;
        final String readPostResponse = writeRead(postData, socket);
        Assert.assertEquals(expectedPostResponse, readPostResponse);

        // create JSON via POST
        final String expectedPostCreationResponse = "HTTP/1.1 201 Created\r\nContent-Length: 0\r\n\r\n";
        final String postCreationBody = "{\"created-by\":\"post\"}";
        final String postCreationData = "POST /post_it.json HTTP/1.1\r\nContent-Length: " + postCreationBody.getBytes(
                UTF_8).length + "\r\n\r\n" + postCreationBody;
        final String readPostCreationResponse = writeRead(postCreationData, socket);
        Assert.assertEquals(expectedPostCreationResponse, readPostCreationResponse);

        // create non JSON via POST
        final String expectedPostNonJsonResponse = "HTTP/1.1 201 Created\r\nContent-Length: 0\r\n\r\n";
        final String postNonJsonBody = "Created by post!";
        final String postNonJsonData = "POST /post_it.txt HTTP/1.1\r\nContent-Length: " + postNonJsonBody.getBytes(
                UTF_8).length + "\r\n\r\n" + postNonJsonBody;
        final String readPostNonJsonResponse = writeRead(postNonJsonData, socket);
        Assert.assertEquals(expectedPostNonJsonResponse, readPostNonJsonResponse);

        // GET updated JSON
        final String expectedGetResponsePrefix = "HTTP/1.1 200 OK\r\nContent-Length: 41";
        final String getData = "GET /putty.json HTTP/1.1\r\n\r\n";
        final String readGetResponse = writeRead(getData, socket);
        final String[] prefixAndBody = readGetResponse.split("\r\n\r\n");
        Assert.assertEquals(prefixAndBody.length, 2);
        final JSONObject readGetBody = new JSONObject(prefixAndBody[1]);
        Assert.assertEquals(expectedGetResponsePrefix, prefixAndBody[0]);
        Assert.assertEquals(readGetBody.keySet().size(), 2);
        Assert.assertEquals(readGetBody.getString("created-by"), "put");
        Assert.assertEquals(readGetBody.getString("modified-by"), "post");

        // DELETE updated JSON
        final String expectedDeleteResponse = "HTTP/1.1 204 No Content\r\n\r\n";
        final String deleteData = "DELETE /putty.json HTTP/1.1\r\n\r\n";
        final String readDeleteResponse = writeRead(deleteData, socket);
        Assert.assertEquals(expectedDeleteResponse, readDeleteResponse);

        socket.close();
    }

    private String openWriteReadClose(@Nonnull final String data) throws IOException
    {
        final SocketChannel socket = openSocket();
        final String readData = writeRead(data, socket);
        socket.close();
        return readData;
    }

    private String writeRead(@Nonnull String data, SocketChannel socket) throws IOException
    {
        writeToSocket(socket, data);
        return readFromSocket(socket);
    }

    private String readFromSocket(@Nonnull final SocketChannel socket) throws IOException
    {
        final ByteBuffer readBuffer = ByteBuffer.allocateDirect(8192);
        final int numBytesRead = socket.read(readBuffer);

        if (numBytesRead == -1)
        {
            LOG.info("Session {} closed. No more data retrieved.", Thread.currentThread().getName());
            return null;
        }
        else
        {
            readBuffer.flip();

            final String readData = UTF_8.decode(readBuffer).toString();
            LOG.info("Client {} read:\n{}", Thread.currentThread().getName(), readData);
            return readData;
        }
    }

    private void writeToSocket(@Nonnull final SocketChannel socket, @Nonnull final String data) throws IOException
    {
        final ByteBuffer writeBuffer = ByteBuffer.allocateDirect(8192);
        writeBuffer.put(data.getBytes(UTF_8));
        writeBuffer.flip();
        socket.write(writeBuffer);
    }

    @Nonnull
    private SocketChannel openSocket() throws IOException
    {
        return SocketChannel.open(new InetSocketAddress(serverConfig.getInterface(), serverConfig.getPort()));
    }
}
