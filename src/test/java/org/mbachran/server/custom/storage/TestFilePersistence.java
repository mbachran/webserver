package org.mbachran.server.custom.storage;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author mba
 */
public class TestFilePersistence
{
    @Test
    public void testTextReadWriteDelete() throws IOException
    {
        final String filename = "TestFilePersistence.txt";
        final String content = "Hello World";

        final FilePersistence persistence = new FilePersistence();
        persistence.writeText(filename, content);
        Assert.assertTrue(Files.exists(Paths.get(persistence.getRootPath(), filename)));
        final String text = persistence.readText(filename);
        Assert.assertEquals(content, text);
        persistence.delete(filename);
        Assert.assertTrue(!Files.exists(Paths.get(persistence.getRootPath(), filename)));
    }

    @Test
    public void testBinaryReadWriteDelete() throws IOException
    {
        final String filename = "TestFilePersistence.bin";
        final byte[] content = "Hello World".getBytes();

        final FilePersistence persistence = new FilePersistence();
        persistence.writeBinary(filename, content);
        Assert.assertTrue(Files.exists(Paths.get(persistence.getRootPath(), filename)));
        final byte[] bytes = persistence.readBinary(filename);
        Assert.assertArrayEquals(content, bytes);
        persistence.delete(filename);
        Assert.assertTrue(!Files.exists(Paths.get(persistence.getRootPath(), filename)));
    }

    @Test
    public void testJsonReadWriteUpdateDelete() throws IOException
    {
        final String filename = "TestFilePersistence.json";
        final String content = "{\"key\":\"value\"}";
        final String update = "{\"key2\":\"value2\", \"nested\": {\"nestedKey\":\"nestedValue\"}}";

        final FilePersistence persistence = new FilePersistence();
        persistence.writeText(filename, content);
        Assert.assertTrue(Files.exists(Paths.get(persistence.getRootPath(), filename)));
        final String text = persistence.readText(filename);
        Assert.assertEquals(content, text);
        persistence.createOrUpdateJson(filename, update);
        final String updated = persistence.readText(filename);
        final JSONObject updatedJson = new JSONObject(updated);
        Assert.assertEquals(updatedJson.get("key"), "value");
        Assert.assertEquals(updatedJson.get("key2"), "value2");
        Assert.assertNotNull(updatedJson.getJSONObject("nested"));
        Assert.assertEquals(updatedJson.getJSONObject("nested").get("nestedKey"), "nestedValue");
        persistence.delete(filename);
        Assert.assertTrue(!Files.exists(Paths.get(persistence.getRootPath(), filename)));
    }
}
