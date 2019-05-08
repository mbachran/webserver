package org.mbachran.server.custom.storage;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simple file persistence that reads and writes to the static folder from the resources of its project.
 * It derives the absolute path of this resource from the class-loader resources and was not tested with a packaged jar/war etc.
 * Missing ancestors / directories are created if needed though during write operations.
 * In an IDE when starting Spring Boot within its Gradle project the file operations will happen within the production output folder.
 *
 * For productisation this needs to be revisited to
 * - make the root path simply configurable
 * - ensure the relative paths stay within the root folder to avoid writing and reading anywhere.
 *
 * This persistence can write/read
 * - binary data without damaging it by any encoding conversion.
 * - test aka Strings
 *
 * There is a special method to create or update JSON files. This will perform a merge on the JSON trees like:
 * - colliding names will cause a value overwrite
 * - attributes of type {@link JSONObject} will cause a merge within the subtree
 * - arrays are not merged (no superset of the values) but overwritten
 *
 * So in principal JSON files can be used as an attribute based tree like storage with multi value support per resource aka file.
 */
@Component
public class FilePersistence
{
    private static final String ROOT_RESOURCE = "static";

    private final String rootPath;

    FilePersistence()
    {
        final URL resource = getClass().getClassLoader().getResource(ROOT_RESOURCE);
        if (resource == null)
        {
            throw new IllegalStateException("Root resource is not available!");
        }

        try
        {
            rootPath = Path.of(resource.toURI()).toFile().getAbsolutePath();
        }
        catch (URISyntaxException e)
        {
            throw new IllegalStateException("Persistence root path not available.", e);
        }
    }

    String getRootPath()
    {
        return rootPath;
    }

    String readText(final String relativeResourcePath) throws IOException
    {
        return Files.readString(buildFullPath(relativeResourcePath));
    }

    byte[] readBinary(final String relativeResourcePath) throws IOException
    {
        return Files.readAllBytes(buildFullPath(relativeResourcePath));
    }

    void writeText(final String relativeResourcePath, final String content) throws IOException
    {
        Files.writeString(buildFullPath(relativeResourcePath), content);
    }

    void writeBinary(final String relativeResourcePath, final byte[] content) throws IOException
    {
        Files.write(buildFullPath(relativeResourcePath), content);
    }

    public void delete(final String relativeResourcePath) throws IOException
    {
        Files.delete(buildFullPath(relativeResourcePath));
    }

    boolean createOrUpdateJson(final String relativeResourcePath, final String update) throws IOException
    {
        final Path path = buildFullPath(relativeResourcePath);
        createMissing(path);

        final String content = Files.readString(path);

        final boolean created;
        final String updatedContent;
        if (!StringUtils.isEmpty(content))
        {
            final JSONObject target = fromJsonString(content);
            final JSONObject source = fromJsonString(update);
            merge(source, target);
            updatedContent = toJsonString(target);
            created = false;
        }
        else
        {
            updatedContent = update;
            created = true;
        }

        Files.writeString(path, updatedContent);
        return created;
    }

    private void merge(final JSONObject source, final JSONObject target)
    {
        for (final String name : source.keySet())
        {
            final Object value = source.get(name);
            if (value instanceof JSONObject)
            {
                JSONObject nested = target.optJSONObject(name);
                if (nested == null)
                {
                    nested = new JSONObject();
                    target.put(name, nested);
                }

                merge((JSONObject) value, nested);
            }
            else
            {
                target.put(name, source.get(name));
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createMissing(Path path) throws IOException
    {
        final File file = path.toFile();
        if (!file.exists())
        {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
    }

    private Path buildFullPath(final String relativeResourcePath)
    {
        return Paths.get(rootPath, relativeResourcePath);
    }

    private JSONObject fromJsonString(final String json)
    {
        return new JSONObject(json);
    }

    private String toJsonString(final JSONObject json)
    {
        return json.toString();
    }
}
