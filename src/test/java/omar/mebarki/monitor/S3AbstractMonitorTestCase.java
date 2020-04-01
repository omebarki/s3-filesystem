/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package omar.mebarki.monitor;

import com.google.common.collect.ImmutableMap;
import com.upplication.s3fs.AmazonS3Factory;
import omar.mebarki.demo.FileAlterationListenerImpl;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.text.MessageFormat;
import java.util.Map;

import static org.apache.commons.io.testtools.TestUtils.sleepQuietly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link NIOFileAlterationObserver} Test Case.
 */
public abstract class S3AbstractMonitorTestCase {
    protected static FileSystem s3fs;
    protected Path tempDir;
    protected String s3BucketName = "omar"; // example bucket name

    /**
     * File observer
     */
    protected NIOFileAlterationObserver observer;

    /**
     * Listener which collects file changes
     */
    protected CollectionFileListener listener;


    /**
     * Time in milliseconds to pause in tests
     */
    protected long pauseTime = 100L;

    @BeforeEach
    public void setUp() throws Exception {
        String s3AccessKey = "newAccessKey";
        String s3SecretKey = "newSecretKey";
        String s3Server = "192.168.160.129:8000"; // example server name

        Map<String, ?> env = ImmutableMap.<String, Object>builder()
                .put(AmazonS3Factory.PROTOCOL, "HTTP")
                .build();

        if (s3fs == null) {
            URI uri = URI.create(MessageFormat.format("s3://{0}:{1}@{2}", s3AccessKey, s3SecretKey, s3Server));
            s3fs = FileSystems.newFileSystem(uri, env);
        }

        tempDir = s3fs.getPath("/" + s3BucketName, "tempDir");
        if (Files.exists(tempDir)) {
            deleteDirectoryRecursion(tempDir);
        }
        Files.createDirectory(tempDir);

        final NIOFileFilter files = p -> (!Files.isDirectory(p));
        final NIOFileFilter javaSuffix = p -> (p.toString().endsWith(".java"));
        final NIOFileFilter fileFilter = p -> (files.accept(p) && javaSuffix.accept(p));

        final NIOFileFilter directories = p -> (Files.isDirectory(p));
        final NIOFileFilter visible = p -> (!Files.isHidden(p));
        final NIOFileFilter dirFilter = p -> (directories.accept(p) && visible.accept(p));

        final NIOFileFilter filter = p -> (dirFilter.accept(p) || fileFilter.accept(p));

        createObserver(tempDir, filter);//TODO filtre
    }

    protected void createObserver(final Path file) {
        observer = new NIOFileAlterationObserver(file);
        observer.addListener(listener);
        observer.addListener(new FileAlterationListenerImpl());
        try {
            observer.initialize();
        } catch (final Exception e) {
            fail("Observer init() threw " + e);
        }
    }

    /**
     * Create a {@link NIOFileAlterationObserver}.
     *
     * @param file       The directory to observe
     * @param fileFilter The file filter to apply
     */
    protected void createObserver(final Path file, final NIOFileFilter fileFilter) {
        observer = new NIOFileAlterationObserver(file, fileFilter);
        observer.addListener(listener);
        observer.addListener(new NIOFileAlterationListenerAdaptor());
        try {
            observer.initialize();
        } catch (final Exception e) {
            fail("Observer init() threw " + e);
        }
    }

    /**
     * Check all the Collections are empty
     *
     * @param label the label to use for this check
     */
    protected void checkCollectionsEmpty(final String label) {
        checkCollectionSizes("EMPTY-" + label, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Check all the Collections have the expected sizes.
     *
     * @param label      the label to use for this check
     * @param dirCreate  expected number of dirs created
     * @param dirChange  expected number of dirs changed
     * @param dirDelete  expected number of dirs deleted
     * @param fileCreate expected number of files created
     * @param fileChange expected number of files changed
     * @param fileDelete expected number of files deleted
     */
    protected void checkCollectionSizes(String label,
                                        final int dirCreate,
                                        final int dirChange,
                                        final int dirDelete,
                                        final int fileCreate,
                                        final int fileChange,
                                        final int fileDelete) {
        label = label + "[" + listener.getCreatedDirectories().size() +
                " " + listener.getChangedDirectories().size() +
                " " + listener.getDeletedDirectories().size() +
                " " + listener.getCreatedFiles().size() +
                " " + listener.getChangedFiles().size() +
                " " + listener.getDeletedFiles().size() + "]";
        assertEquals(dirCreate, listener.getCreatedDirectories().size(), label + ": No. of directories created");
        assertEquals(dirChange, listener.getChangedDirectories().size(), label + ": No. of directories changed");
        assertEquals(dirDelete, listener.getDeletedDirectories().size(), label + ": No. of directories deleted");
        assertEquals(fileCreate, listener.getCreatedFiles().size(), label + ": No. of files created");
        assertEquals(fileChange, listener.getChangedFiles().size(), label + ": No. of files changed");
        assertEquals(fileDelete, listener.getDeletedFiles().size(), label + ": No. of files deleted");
    }

    /**
     * Either creates a file if it doesn't exist or updates the last modified date/time
     * if it does.
     *
     * @param file The file to touch
     * @return The file
     */
    protected Path touch(Path file) {
        long lastModified = 0;
        try {

            file = touchPath(file);
            while (lastModified == getLastModifiedTime(file)) {
                sleepQuietly(pauseTime);
                file = touchPath(file);
            }
        } catch (final Exception e) {
            fail("Touching " + file + ": " + e);
        }
        sleepQuietly(pauseTime);
        return file;
    }

    public static Path touchPath(Path file) throws IOException {

        if (!Files.exists(file)) {
            file = Files.createFile(file);
        }
        if (!Files.isDirectory(file)) {
            Files.delete(file);
            file = Files.createFile(file);
        }
        return file;
    }

    protected Path touchDir(Path file) {
        long lastModified = 0;
        try {

            file = touchPathDir(file);
            while (lastModified == getLastModifiedTime(file)) {
                sleepQuietly(pauseTime);
                file = touchPathDir(file);
            }
        } catch (final Exception e) {
            fail("Touching " + file + ": " + e);
        }
        sleepQuietly(pauseTime);
        return file;
    }

    public Path touchPathDir(Path file) throws IOException {
        if (!Files.exists(file)) {
            file = Files.createDirectory(file);
        }
        if (Files.isDirectory(file)) {
            deleteDirectoryRecursion(file);
            file = Files.createDirectory(file);
        }
        return file;
    }

    public long getLastModifiedTime(Path file) {
        long lastModified = 0;
        try {
            lastModified = Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : 0;
        } catch (IOException e) {
        }
        return lastModified;
    }

    void deleteDirectoryRecursion(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        Files.delete(path);
    }

}
