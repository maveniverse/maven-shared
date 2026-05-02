package eu.maveniverse.maven.shared.core.fs;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class FileUtilsTest {
    @Test
    void javaHome() throws IOException {
        Path path = FileUtils.discoverCanonicalDirectoryFromSystemProperty("java.home", null);
        assertNotNull(path);
    }
}
