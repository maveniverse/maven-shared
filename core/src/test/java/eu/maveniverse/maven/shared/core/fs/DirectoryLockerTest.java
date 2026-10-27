/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.shared.core.fs;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DirectoryLockerTest {
    @Test
    void sharedShared(@TempDir Path basedir) throws IOException {
        DirectoryLocker locker = DirectoryLocker.INSTANCE;

        try {
            locker.lockDirectory(basedir, false);
            locker.lockDirectory(basedir, false);
        } finally {
            locker.unlockDirectory(basedir);
            locker.unlockDirectory(basedir);
        }
    }

    @Test
    void exclusiveShared(@TempDir Path basedir) throws IOException {
        DirectoryLocker locker = DirectoryLocker.INSTANCE;

        try {
            locker.lockDirectory(basedir, true);
            assertThrows(IOException.class, () -> locker.lockDirectory(basedir, false));
        } finally {
            locker.unlockDirectory(basedir);
        }
    }

    @Test
    void sharedExclusive(@TempDir Path basedir) throws IOException {
        DirectoryLocker locker = DirectoryLocker.INSTANCE;

        try {
            locker.lockDirectory(basedir, false);
            assertThrows(IOException.class, () -> locker.lockDirectory(basedir, true));
        } finally {
            locker.unlockDirectory(basedir);
        }
    }

    @Test
    void failedSharedKeepsExclusiveLock(@TempDir Path basedir) throws IOException {
        DirectoryLocker locker = DirectoryLocker.INSTANCE;

        try {
            locker.lockDirectory(basedir, true);
            assertThrows(IOException.class, () -> locker.lockDirectory(basedir, false));
            try (FileChannel channel = FileChannel.open(basedir.resolve(".lock"), StandardOpenOption.WRITE)) {
                // lock still held by this JVM
                assertThrows(OverlappingFileLockException.class, channel::tryLock);
            }
        } finally {
            locker.unlockDirectory(basedir);
        }
    }

    @Test
    void failedLockDoesNotPoisonDirectory(@TempDir Path basedir) throws IOException {
        DirectoryLocker locker = DirectoryLocker.INSTANCE;

        Files.createFile(basedir.resolve(".lock"));
        try (FileChannel channel = FileChannel.open(basedir.resolve(".lock"), StandardOpenOption.WRITE);
                FileLock ignored = channel.lock()) {
            assertThrows(IOException.class, () -> locker.lockDirectory(basedir, true));
        }
        locker.lockDirectory(basedir, true);
        locker.unlockDirectory(basedir);
    }

    @Test
    void differentPathRepresentations(@TempDir Path basedir) throws IOException {
        DirectoryLocker locker = DirectoryLocker.INSTANCE;
        Path other = basedir.resolve("x").resolve("..");

        try {
            locker.lockDirectory(basedir, true);
            assertThrows(IOException.class, () -> locker.lockDirectory(other, true));
        } finally {
            locker.unlockDirectory(other);
        }
        locker.lockDirectory(basedir, false);
        locker.lockDirectory(other, false);
        locker.unlockDirectory(basedir);
        locker.unlockDirectory(other);
        assertThrows(IOException.class, () -> locker.unlockDirectory(basedir));
    }

    @Test
    void exclusiveExclusive(@TempDir Path basedir) throws IOException {
        DirectoryLocker locker = DirectoryLocker.INSTANCE;

        try {
            locker.lockDirectory(basedir, true);
            assertThrows(IOException.class, () -> locker.lockDirectory(basedir, true));
        } finally {
            locker.unlockDirectory(basedir);
        }
    }
}
