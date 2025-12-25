package com.voyager.crawler;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class CrawlerApplicationTest {

    @Test
    void testParseArguments_ValidOrder() {
        Object cli = invokeParseArguments("https://example.com", "5", "2", "false");

        URI seedUrl = (URI) invokeAccessor(cli, "seedUrl");
        int maxLinksPerPage = (int) invokeAccessor(cli, "maxLinksPerPage");
        int maxDepth = (int) invokeAccessor(cli, "maxDepth");
        boolean isUnique = (boolean) invokeAccessor(cli, "isUnique");

        assertEquals(URI.create("https://example.com"), seedUrl);
        assertEquals(5, maxLinksPerPage);
        assertEquals(2, maxDepth);
        assertFalse(isUnique);
    }

    @Test
    void testParseArguments_RequiresFourArgs() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> invokeParseArguments("https://example.com", "5", "2"));
        assertEquals("Expected 4 arguments.", ex.getMessage());
    }

    private static Object invokeParseArguments(String... args) {
        try {
            Method method = CrawlerApplication.class.getDeclaredMethod("parseArguments", String[].class);
            method.setAccessible(true);
            return method.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object invokeAccessor(Object target, String name) {
        try {
            Method method = target.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
