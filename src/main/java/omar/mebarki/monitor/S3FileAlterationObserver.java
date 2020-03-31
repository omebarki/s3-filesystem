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


import java.io.IOException;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class S3FileAlterationObserver implements Serializable {

    private static Path[] EMPTY_FILE_ARRAY = new Path[0];
    private static final long serialVersionUID = 1185122225658782848L;
    private final List<S3FileAlterationListener> listeners = new CopyOnWriteArrayList<>();
    private final S3FileEntry rootEntry;
    private final S3FileFilter fileFilter;
    private final Comparator<Path> comparator;

    /**
     * Construct an observer for the specified directory.
     *
     * @param directoryName the name of the directory to observe
     */
    public S3FileAlterationObserver(final String directoryName, FileSystem s3fs) {
        this(s3fs.getPath(directoryName));
    }

    /**
     * Construct an observer for the specified directory and file filter.
     *
     * @param directoryName the name of the directory to observe
     * @param fileFilter    The file filter or null if none
     */
    public S3FileAlterationObserver(final String directoryName, FileSystem s3fs, final S3FileFilter fileFilter) {
        this(s3fs.getPath(directoryName), fileFilter);
    }

    /**
     * Construct an observer for the specified directory.
     *
     * @param directory the directory to observe
     */
    public S3FileAlterationObserver(final Path directory) {
        this(directory, null);
    }

    /**
     * Construct an observer for the specified directory, file filter and
     * file comparator.
     *
     * @param directory       the directory to observe
     * @param fileFilter      The file filter or null if none
     */
    public S3FileAlterationObserver(final Path directory, final S3FileFilter fileFilter) {
        this(new S3FileEntry(directory), fileFilter);
    }

    /**
     * Construct an observer for the specified directory, file filter and
     * file comparator.
     *
     * @param rootEntry       the root directory to observe
     * @param fileFilter      The file filter or null if none
     */
    protected S3FileAlterationObserver(final S3FileEntry rootEntry, final S3FileFilter fileFilter) {
        if (rootEntry == null) {
            throw new IllegalArgumentException("Root entry is missing");
        }
        if (rootEntry.getFile() == null) {
            throw new IllegalArgumentException("Root directory is missing");
        }
        this.rootEntry = rootEntry;
        this.fileFilter = fileFilter;
        this.comparator = (f1, f2) -> f1.getFileName().compareTo(f2.getFileName());

    }

    /**
     * Return the directory being observed.
     *
     * @return the directory being observed
     */
    public Path getDirectory() {
        return rootEntry.getFile();
    }

    /**
     * Return the fileFilter.
     *
     * @return the fileFilter
     * @since 2.1
     */
    public S3FileFilter getFileFilter() {
        return fileFilter;
    }

    /**
     * Add a file system listener.
     *
     * @param listener The file system listener
     */
    public void addListener(final S3FileAlterationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a file system listener.
     *
     * @param listener The file system listener
     */
    public void removeListener(final S3FileAlterationListener listener) {
        if (listener != null) {
            while (listeners.remove(listener)) {
            }
        }
    }

    /**
     * Returns the set of registered file system listeners.
     *
     * @return The file system listeners
     */
    public Iterable<S3FileAlterationListener> getListeners() {
        return listeners;
    }

    /**
     * Initialize the observer.
     *
     * @throws Exception if an error occurs
     */
    public void initialize() throws Exception {
        rootEntry.refresh(rootEntry.getFile());
        final S3FileEntry[] children = doListFiles(rootEntry.getFile(), rootEntry);
        rootEntry.setChildren(children);
    }

    /**
     * Final processing.
     *
     * @throws Exception if an error occurs
     */
    public void destroy() throws Exception {
    }

    /**
     * Check whether the file and its children have been created, modified or deleted.
     */
    public void checkAndNotify() {

        /* fire onStart() */
        for (final S3FileAlterationListener listener : listeners) {
            listener.onStart(this);
        }

        /* fire directory/file events */
        final Path rootFile = rootEntry.getFile();
        if (Files.exists(rootFile)) {
            checkAndNotify(rootEntry, rootEntry.getChildren(), listFiles(rootFile));
        } else if (rootEntry.isExists()) {
            checkAndNotify(rootEntry, rootEntry.getChildren(), EMPTY_FILE_ARRAY);
        } else {
            // Didn't exist and still doesn't
        }

        /* fire onStop() */
        for (final S3FileAlterationListener listener : listeners) {
            listener.onStop(this);
        }
    }

    /**
     * Compare two file lists for files which have been created, modified or deleted.
     *
     * @param parent   The parent entry
     * @param previous The original list of files
     * @param files    The current list of files
     */
    private void checkAndNotify(final S3FileEntry parent, final S3FileEntry[] previous, final Path[] files) {
        int c = 0;
        final S3FileEntry[] current = files.length > 0 ? new S3FileEntry[files.length] : S3FileEntry.EMPTY_ENTRIES;
        for (final S3FileEntry entry : previous) {
            while (c < files.length && comparator.compare(entry.getFile(), files[c]) > 0) {
                current[c] = createS3FileEntry(parent, files[c]);
                doCreate(current[c]);
                c++;
            }
            if (c < files.length && comparator.compare(entry.getFile(), files[c]) == 0) {
                doMatch(entry, files[c]);
                checkAndNotify(entry, entry.getChildren(), listFiles(files[c]));
                current[c] = entry;
                c++;
            } else {
                checkAndNotify(entry, entry.getChildren(), EMPTY_FILE_ARRAY);
                doDelete(entry);
            }
        }
        for (; c < files.length; c++) {
            current[c] = createS3FileEntry(parent, files[c]);
            doCreate(current[c]);
        }
        parent.setChildren(current);
    }

    /**
     * Create a new file entry for the specified file.
     *
     * @param parent The parent file entry
     * @param file   The file to create an entry for
     * @return A new file entry
     */
    private S3FileEntry createS3FileEntry(final S3FileEntry parent, final Path file) {
        final S3FileEntry entry = parent.newChildInstance(file);
        entry.refresh(file);
        final S3FileEntry[] children = doListFiles(file, entry);
        entry.setChildren(children);
        return entry;
    }

    /**
     * List the files
     *
     * @param file  The file to list files for
     * @param entry the parent entry
     * @return The child files
     */
    private S3FileEntry[] doListFiles(final Path file, final S3FileEntry entry) {
        final Path[] files = listFiles(file);
        final S3FileEntry[] children = files.length > 0 ? new S3FileEntry[files.length] : S3FileEntry.EMPTY_ENTRIES;
        for (int i = 0; i < files.length; i++) {
            children[i] = createS3FileEntry(entry, files[i]);
        }
        return children;
    }

    /**
     * Fire directory/file created events to the registered listeners.
     *
     * @param entry The file entry
     */
    private void doCreate(final S3FileEntry entry) {
        for (final S3FileAlterationListener listener : listeners) {
            if (entry.isDirectory()) {
                listener.onDirectoryCreate(entry.getFile());
            } else {
                listener.onFileCreate(entry.getFile());
            }
        }
        final S3FileEntry[] children = entry.getChildren();
        for (final S3FileEntry aChildren : children) {
            doCreate(aChildren);
        }
    }

    /**
     * Fire directory/file change events to the registered listeners.
     *
     * @param entry The previous file system entry
     * @param file  The current file
     */
    private void doMatch(final S3FileEntry entry, final Path file) {
        if (entry.refresh(file)) {
            for (final S3FileAlterationListener listener : listeners) {
                if (entry.isDirectory()) {
                    listener.onDirectoryChange(file);
                } else {
                    listener.onFileChange(file);
                }
            }
        }
    }

    /**
     * Fire directory/file delete events to the registered listeners.
     *
     * @param entry The file entry
     */
    private void doDelete(final S3FileEntry entry) {
        for (final S3FileAlterationListener listener : listeners) {
            if (entry.isDirectory()) {
                listener.onDirectoryDelete(entry.getFile());
            } else {
                listener.onFileDelete(entry.getFile());
            }
        }
    }

    /**
     * List the contents of a directory
     *
     * @param file The file to list the contents of
     * @return the directory contents or a zero length array if
     * the empty or the file is not a directory
     */
    private Path[] listFiles(final Path file) {
        Path[] children = null;
        try {
            if (Files.isDirectory(file)) {
                children = fileFilter == null ? listDir(file) : listDir(file, fileFilter);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (children == null) {
            children = EMPTY_FILE_ARRAY;
        }
        if (comparator != null && children.length > 1) {
            Arrays.sort(children, comparator);
        }
        return children;
    }

    public Path[] listDir(Path dir) throws IOException {
        return listDir(dir, null);
    }

    public Path[] listDir(Path dir, S3FileFilter fileFilter) throws IOException {
        List<Path> fileList = new ArrayList<>();
        Path[] files = new Path[0];
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (fileFilter != null) {
                    if (fileFilter.accept(path)) {
                        fileList.add(path);
                    }
                } else {
                    fileList.add(path);
                }
            }
        }
        return fileList.toArray(files);
    }

    /**
     * Provide a String representation of this observer.
     *
     * @return a String representation of this observer
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append("[file='");
        builder.append(getDirectory().toString());//TODO voir   avec S3
        builder.append('\'');
        if (fileFilter != null) {
            builder.append(", ");
            builder.append(fileFilter.toString());
        }
        builder.append(", listeners=");
        builder.append(listeners.size());
        builder.append("]");
        return builder.toString();
    }

}
