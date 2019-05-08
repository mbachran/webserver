package org.mbachran.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Nonnull;

/**
 * Starts the server using Spring Boot to enable dependency injection.
 * The embedded web-server is meant to be disabled. But this is up to the application properties.
 *
 * Spring provides dependency injection only to the server.
 * {@link org.mbachran.server.custom.Server} is the bean that will start the custom server and the root of the rest.
 */
@SpringBootApplication
public class ServerApplication
{
    public static void main(@Nonnull final String[] args)
    {
        SpringApplication.run(ServerApplication.class, args);
    }
}
