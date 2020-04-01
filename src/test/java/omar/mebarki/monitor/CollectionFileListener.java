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

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

/**
 * {@link NIOFileAlterationListener} implementation that adds created, changed and deleted
 * files/directories to a set of {@link Collection}s.
 */
public class CollectionFileListener implements NIOFileAlterationListener, Serializable {

    private static final long serialVersionUID = 939724715678693963L;
    private final boolean clearOnStart;
    private final Collection<Path> createdFiles = new ArrayList<>();
    private final Collection<Path> changedFiles = new ArrayList<>();
    private final Collection<Path> deletedFiles = new ArrayList<>();
    private final Collection<Path> createdDirectories = new ArrayList<>();
    private final Collection<Path> changedDirectories = new ArrayList<>();
    private final Collection<Path> deletedDirectories = new ArrayList<>();

    /**
     * Create a new observer.
     *
     * @param clearOnStart true if clear() should be called by onStart().
     */
    public CollectionFileListener(final boolean clearOnStart) {
        this.clearOnStart = clearOnStart;
    }

    /**
     * File system observer started checking event.
     *
     * @param observer The file system observer
     */
    @Override
    public void onStart(final NIOFileAlterationObserver observer) {
        if (clearOnStart) {
            clear();
        }
    }

    /**
     * Clear file collections.
     */
    public void clear() {
        createdFiles.clear();
        changedFiles.clear();
        deletedFiles.clear();
        createdDirectories.clear();
        changedDirectories.clear();
        deletedDirectories.clear();
    }

    /**
     * Return the set of changed directories.
     *
     * @return Directories which have changed
     */
    public Collection<Path> getChangedDirectories() {
        return changedDirectories;
    }

    /**
     * Return the set of changed files.
     *
     * @return Files which have changed
     */
    public Collection<Path> getChangedFiles() {
        return changedFiles;
    }

    /**
     * Return the set of created directories.
     *
     * @return Directories which have been created
     */
    public Collection<Path> getCreatedDirectories() {
        return createdDirectories;
    }

    /**
     * Return the set of created files.
     *
     * @return Files which have been created
     */
    public Collection<Path> getCreatedFiles() {
        return createdFiles;
    }

    /**
     * Return the set of deleted directories.
     *
     * @return Directories which been deleted
     */
    public Collection<Path> getDeletedDirectories() {
        return deletedDirectories;
    }

    /**
     * Return the set of deleted files.
     *
     * @return Files which been deleted
     */
    public Collection<Path> getDeletedFiles() {
        return deletedFiles;
    }

    /**
     * Directory created Event.
     *
     * @param directory The directory created
     */
    @Override
    public void onDirectoryCreate(final Path directory) {
        createdDirectories.add(directory);
    }

    /**
     * Directory changed Event.
     *
     * @param directory The directory changed
     */
    @Override
    public void onDirectoryChange(final Path directory) {
        changedDirectories.add(directory);
    }

    /**
     * Directory deleted Event.
     *
     * @param directory The directory deleted
     */
    @Override
    public void onDirectoryDelete(final Path directory) {
        deletedDirectories.add(directory);
    }

    /**
     * File created Event.
     *
     * @param file The file created
     */
    @Override
    public void onFileCreate(final Path file) {
        createdFiles.add(file);
    }

    /**
     * File changed Event.
     *
     * @param file The file changed
     */
    @Override
    public void onFileChange(final Path file) {
        changedFiles.add(file);
    }

    /**
     * File deleted Event.
     *
     * @param file The file deleted
     */
    @Override
    public void onFileDelete(final Path file) {
        deletedFiles.add(file);
    }

    /**
     * File system observer finished checking event.
     *
     * @param observer The file system observer
     */
    @Override
    public void onStop(final NIOFileAlterationObserver observer) {
        // noop
    }

}
