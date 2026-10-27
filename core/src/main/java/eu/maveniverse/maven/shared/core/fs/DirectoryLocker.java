/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.shared.core.fs;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

public final class DirectoryLocker {
    public static final DirectoryLocker INSTANCE = new DirectoryLocker();

    private final HashMap<Path, FileChannel> fileChannels = new HashMap<>();
    private final HashMap<Path, FileLock> fileLocks = new HashMap<>();
    private final HashMap<Path, Integer> references = new HashMap<>();

    private DirectoryLocker() {}

    public synchronized void lockDirectory(Path directory, boolean exclusiveAccess) throws IOException {
        requireNonNull(directory, "directory");
        directory = directory.toAbsolutePath().normalize();

        Path lockFile = directory.resolve(".lock");
        if (!Files.isRegularFile(lockFile)) {
            try {
                Files.createFile(lockFile);
            } catch (FileAlreadyExistsException e) {
                // ignore
            }
        }
        FileLock lock = fileLocks.get(directory);
        if (lock != null) {
            // already locked by this JVM: only shared access may be shared
            if (exclusiveAccess || !lock.isShared()) {
                throw new IOException("Failed to gain " + (exclusiveAccess ? "exclusive" : "shared")
                        + " access to storage: " + directory);
            }
        } else {
            FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.READ, StandardOpenOption.WRITE);
            try {
                lock = channel.tryLock(0, Long.MAX_VALUE, !exclusiveAccess);
            } catch (OverlappingFileLockException e) {
                // locked by some other channel in this JVM
                lock = null;
            } catch (IOException | RuntimeException e) {
                channel.close();
                throw e;
            }
            if (lock == null || lock.isShared() == exclusiveAccess) {
                if (lock != null) {
                    lock.release();
                }
                channel.close();
                throw new IOException("Failed to gain " + (exclusiveAccess ? "exclusive" : "shared")
                        + " access to storage: " + directory);
            }
            fileChannels.put(directory, channel);
            fileLocks.put(directory, lock);
        }
        references.merge(directory, 1, Integer::sum);
    }

    public synchronized void unlockDirectory(Path directory) throws IOException {
        requireNonNull(directory, "directory");
        directory = directory.toAbsolutePath().normalize();
        Integer refs = references.get(directory);
        if (refs == null) {
            throw new IOException("Directory was not locked: " + directory);
        }
        if (refs > 1) {
            references.put(directory, refs - 1);
            return;
        }
        references.remove(directory);
        try {
            fileLocks.remove(directory).close();
        } catch (IOException e) {
            // ignore
        }
        try {
            fileChannels.remove(directory).close();
        } catch (IOException e) {
            // ignore
        }
    }
}
