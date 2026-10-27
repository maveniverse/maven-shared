/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.shared.core.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileUtilsTest {
    @Test
    void javaHome() throws IOException {
        Path path = FileUtils.discoverCanonicalDirectoryFromSystemProperty("java.home", null);
        assertNotNull(path);
    }

    @Test
    void canonicalPathOfRelativeNonExistent() {
        Path path = FileUtils.canonicalPath(Paths.get("does-not-exist"));
        assertEquals(FileUtils.discoverUserCurrentWorkingDirectory().resolve("does-not-exist"), path);
    }

    @Test
    void collocatedTempFileRemovedWhenMoveFails(@TempDir Path basedir) throws IOException {
        // target is a non-empty directory: move over it fails
        Path target = basedir.resolve("target");
        Files.createDirectories(target.resolve("child"));
        Path[] temp = new Path[1];
        assertThrows(IOException.class, () -> {
            try (FileUtils.CollocatedTempFile tempFile = FileUtils.newCollocatedTempFile(target)) {
                temp[0] = tempFile.getPath();
                Files.write(temp[0], new byte[] {1});
                tempFile.move();
            }
        });
        assertFalse(Files.exists(temp[0]));
    }
}
