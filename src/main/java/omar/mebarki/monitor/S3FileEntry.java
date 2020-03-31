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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;


public class S3FileEntry implements Serializable {

    private static final long serialVersionUID = -2505664948818681153L;

    static final S3FileEntry[] EMPTY_ENTRIES = new S3FileEntry[0];

    private final S3FileEntry parent;
    private S3FileEntry[] children;
    private final Path file;
    private String name;
    private boolean exists;
    private boolean directory;
    private long lastModified;
    private long length;

    /**
     * Construct a new monitor for a specified {@link Path}.
     *
     * @param file The file being monitored
     */
    public S3FileEntry(final Path file) {
        this(null, file);
    }

    /**
     * Construct a new monitor for a specified {@link Path}.
     *
     * @param parent The parent
     * @param file   The file being monitored
     */
    public S3FileEntry(final S3FileEntry parent, final Path file) {
        if (file == null) {
            throw new IllegalArgumentException("Path is missing");
        }
        this.file = file;
        this.parent = parent;
        this.name = file.getFileName() == null ? "/" : file.getFileName().toString();
    }

    /**
     * Refresh the attributes from the {@link Path}, indicating
     * whether the file has changed.
     * <p>
     * This implementation refreshes the <code>name</code>, <code>exists</code>,
     * <code>directory</code>, <code>lastModified</code> and <code>length</code>
     * properties.
     * <p>
     * The <code>exists</code>, <code>directory</code>, <code>lastModified</code>
     * and <code>length</code> properties are compared for changes
     *
     * @param file the file instance to compare to
     * @return {@code true} if the file has changed, otherwise {@code false}
     */
    public boolean refresh(final Path file) {

        // cache original values
        final boolean origExists = exists;
        final long origLastModified = lastModified;
        final boolean origDirectory = directory;
        final long origLength = length;

        // refresh the values
        name = file.getFileName() == null ? "/" : file.getFileName().toString();
        exists = Files.exists(file);
        directory = exists && Files.isDirectory(file);
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(file);
            long lastModifiedTimeMs = (lastModifiedTime == null) ? 0L : lastModifiedTime.toMillis();
            lastModified = exists ? lastModifiedTimeMs : 0;
        } catch (IOException e) {
            lastModified = 0;
            e.printStackTrace();
        }
        try {
            length = exists && !directory ? Files.size(file) : 0;
        } catch (IOException e) {
            length = 0;
            e.printStackTrace();
        }

        // Return if there are changes
        return exists != origExists ||
                lastModified != origLastModified ||
                directory != origDirectory ||
                length != origLength;
    }

    /**
     * Create a new child instance.
     * <p>
     * Custom implementations should override this method to return
     * a new instance of the appropriate type.
     *
     * @param file The child file
     * @return a new child instance
     */
    public S3FileEntry newChildInstance(final Path file) {
        return new S3FileEntry(this, file);
    }

    /**
     * Return the parent entry.
     *
     * @return the parent entry
     */
    public S3FileEntry getParent() {
        return parent;
    }

    /**
     * Return the level
     *
     * @return the level
     */
    public int getLevel() {
        return parent == null ? 0 : parent.getLevel() + 1;
    }

    /**
     * Return the directory's files.
     *
     * @return This directory's files or an empty
     * array if the file is not a directory or the
     * directory is empty
     */
    public S3FileEntry[] getChildren() {
        return children != null ? children : EMPTY_ENTRIES;
    }

    /**
     * Set the directory's files.
     *
     * @param children This directory's files, may be null
     */
    public void setChildren(final S3FileEntry[] children) {
        this.children = children;
    }

    /**
     * Return the file being monitored.
     *
     * @return the file being monitored
     */
    public Path getFile() {
        return file;
    }

    /**
     * Return the file name.
     *
     * @return the file name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the file name.
     *
     * @param name the file name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Return the last modified time from the last time it
     * was checked.
     *
     * @return the last modified time
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Return the last modified time from the last time it
     * was checked.
     *
     * @param lastModified The last modified time
     */
    public void setLastModified(final long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Return the length.
     *
     * @return the length
     */
    public long getLength() {
        return length;
    }

    /**
     * Set the length.
     *
     * @param length the length
     */
    public void setLength(final long length) {
        this.length = length;
    }

    /**
     * Indicate whether the file existed the last time it
     * was checked.
     *
     * @return whether the file existed
     */
    public boolean isExists() {
        return exists;
    }

    /**
     * Set whether the file existed the last time it
     * was checked.
     *
     * @param exists whether the file exists or not
     */
    public void setExists(final boolean exists) {
        this.exists = exists;
    }

    /**
     * Indicate whether the file is a directory or not.
     *
     * @return whether the file is a directory or not
     */
    public boolean isDirectory() {
        return directory;
    }

    /**
     * Set whether the file is a directory or not.
     *
     * @param directory whether the file is a directory or not
     */
    public void setDirectory(final boolean directory) {
        this.directory = directory;
    }
}
