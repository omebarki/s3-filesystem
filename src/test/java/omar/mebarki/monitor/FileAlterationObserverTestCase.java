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


import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link NIOFileAlterationObserver} Test Case.
 */
public class FileAlterationObserverTestCase extends AbstractMonitorTestCase {

    /**
     * Construct a new test case.
     */
    public FileAlterationObserverTestCase() {
        listener = new CollectionFileListener(true);
    }

    /**
     * Test add/remove listeners.
     */
    @Test
    public void testAddRemoveListeners() {
        final NIOFileAlterationObserver observer = new NIOFileAlterationObserver(Paths.get("/foo"));
        // Null Listener
        observer.addListener(null);
        assertFalse(observer.getListeners().iterator().hasNext(), "Listeners[1]");
        observer.removeListener(null);
        assertFalse(observer.getListeners().iterator().hasNext(), "Listeners[2]");

        // Add Listener
        final NIOFileAlterationListenerAdaptor listener = new NIOFileAlterationListenerAdaptor();
        observer.addListener(listener);
        final Iterator<NIOFileAlterationListener> it = observer.getListeners().iterator();
        assertTrue(it.hasNext(), "Listeners[3]");
        assertEquals(listener, it.next(), "Added");
        assertFalse(it.hasNext(), "Listeners[4]");

        // Remove Listener
        observer.removeListener(listener);
        assertFalse(observer.getListeners().iterator().hasNext(), "Listeners[5]");
    }

    /**
     * Test toString().
     */
    @Test
    public void testToString() {
        final Path file = Paths.get("/foo");
        NIOFileAlterationObserver observer = null;

        observer = new NIOFileAlterationObserver(file);
        assertEquals("NIOFileAlterationObserver[file='" + file + "', listeners=0]",
                observer.toString());

        observer = new NIOFileAlterationObserver(file, p -> Files.isReadable(p));
        assertTrue(observer.toString().startsWith("NIOFileAlterationObserver[file='" + file));

        assertEquals(file, observer.getDirectory());
    }

    /**
     * Test checkAndNotify() method
     *
     * @throws Exception
     */
    @Test
    public void testDirectory() throws Exception {
        checkAndNotify();
        checkCollectionsEmpty("A");
        final Path testDirA = testDir.resolve("test-dir-A");
        final Path testDirB = testDir.resolve("test-dir-B");
        final Path testDirC = testDir.resolve("test-dir-C");
        Files.createDirectory(testDirA);
        Files.createDirectory(testDirB);
        Files.createDirectory(testDirC);
        final Path testDirAFile1 = touch(testDirA.resolve("A-file1.java"));
        final Path testDirAFile2 = touch(testDirA.resolve("A-file2.txt")); // filter should ignore this
        final Path testDirAFile3 = touch(testDirA.resolve("A-file3.java"));
        Path testDirAFile4 = touch(testDirA.resolve("A-file4.java"));
        final Path testDirBFile1 = touch(testDirB.resolve("B-file1.java"));

        checkAndNotify();
        checkCollectionSizes("B", 3, 0, 0, 4, 0, 0);
        assertTrue(listener.getCreatedDirectories().contains(testDirA), "B testDirA");
        assertTrue(listener.getCreatedDirectories().contains(testDirB), "B testDirB");
        assertTrue(listener.getCreatedDirectories().contains(testDirC), "B testDirC");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile1), "B testDirAFile1");
        assertFalse(listener.getCreatedFiles().contains(testDirAFile2), "B testDirAFile2");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile3), "B testDirAFile3");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile4), "B testDirAFile4");
        assertTrue(listener.getCreatedFiles().contains(testDirBFile1), "B testDirBFile1");

        checkAndNotify();
        checkCollectionsEmpty("C");

        testDirAFile4 = touch(testDirAFile4);
        deleteDirectoryRecursion(testDirB);
        checkAndNotify();
        checkCollectionSizes("D", 0, 0, 1, 0, 1, 1);
        assertTrue(listener.getDeletedDirectories().contains(testDirB), "D testDirB");
        assertTrue(listener.getChangedFiles().contains(testDirAFile4), "D testDirAFile4");
        assertTrue(listener.getDeletedFiles().contains(testDirBFile1), "D testDirBFile1");

        deleteDirectoryRecursion(testDir);
        checkAndNotify();
        checkCollectionSizes("E", 0, 0, 2, 0, 0, 3);
        assertTrue(listener.getDeletedDirectories().contains(testDirA), "E testDirA");
        assertTrue(listener.getDeletedFiles().contains(testDirAFile1), "E testDirAFile1");
        assertFalse(listener.getDeletedFiles().contains(testDirAFile2), "E testDirAFile2");
        assertTrue(listener.getDeletedFiles().contains(testDirAFile3), "E testDirAFile3");
        assertTrue(listener.getDeletedFiles().contains(testDirAFile4), "E testDirAFile4");

        Files.createDirectory(testDir);
        checkAndNotify();
        checkCollectionsEmpty("F");

        checkAndNotify();
        checkCollectionsEmpty("G");
    }

    /**
     * Test checkAndNotify() creating
     *
     * @throws Exception
     */
    @Test
    public void testFileCreate() throws Exception {
        checkAndNotify();
        checkCollectionsEmpty("A");
        Path testDirA = testDir.resolve("test-dir-A");
        Files.createDirectory(testDirA);
        touch(testDir);
        testDirA = touch(testDirA);
        Path testDirAFile1 = testDirA.resolve("A-file1.java");
        final Path testDirAFile2 = touch(testDirA.resolve("A-file2.java"));
        Path testDirAFile3 = testDirA.resolve("A-file3.java");
        final Path testDirAFile4 = touch(testDirA.resolve("A-file4.java"));
        Path testDirAFile5 = testDirA.resolve("A-file5.java");

        checkAndNotify();
        checkCollectionSizes("B", 1, 0, 0, 2, 0, 0);
        assertFalse(listener.getCreatedFiles().contains(testDirAFile1), "B testDirAFile1");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile2), "B testDirAFile2");
        assertFalse(listener.getCreatedFiles().contains(testDirAFile3), "B testDirAFile3");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile4), "B testDirAFile4");
        assertFalse(listener.getCreatedFiles().contains(testDirAFile5), "B testDirAFile5");

        assertFalse(Files.exists(testDirAFile1), "B testDirAFile1 exists");
        assertTrue(Files.exists(testDirAFile2), "B testDirAFile2 exists");
        assertFalse(Files.exists(testDirAFile3), "B testDirAFile3 exists");
        assertTrue(Files.exists(testDirAFile4), "B testDirAFile4 exists");
        assertFalse(Files.exists(testDirAFile5), "B testDirAFile5 exists");

        checkAndNotify();
        checkCollectionsEmpty("C");

        // Create file with name < first entry
        testDirAFile1 = touch(testDirAFile1);
        testDirA = touch(testDirA);
        checkAndNotify();
        checkCollectionSizes("D", 0, 1, 0, 1, 0, 0);
        assertTrue(Files.exists(testDirAFile1), "D testDirAFile1 exists");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile1), "D testDirAFile1");

        // Create file with name between 2 entries
        testDirAFile3 = touch(testDirAFile3);
        testDirA = touch(testDirA);
        checkAndNotify();
        checkCollectionSizes("E", 0, 1, 0, 1, 0, 0);
        assertTrue(Files.exists(testDirAFile3), "E testDirAFile3 exists");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile3), "E testDirAFile3");

        // Create file with name > last entry
        testDirAFile5 = touch(testDirAFile5);
        testDirA = touch(testDirA);
        checkAndNotify();
        checkCollectionSizes("F", 0, 1, 0, 1, 0, 0);
        assertTrue(Files.exists(testDirAFile5), "F testDirAFile5 exists");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile5), "F testDirAFile5");
    }

    /**
     * Test checkAndNotify() creating
     *
     * @throws Exception
     */
    @Test
    public void testFileUpdate() throws Exception {
        checkAndNotify();
        checkCollectionsEmpty("A");
        Path testDirA = testDir.resolve("test-dir-A");
        Files.createDirectory(testDirA);
        touch(testDir);
        testDirA = touch(testDirA);
        Path testDirAFile1 = touch(testDirA.resolve("A-file1.java"));
        final Path testDirAFile2 = touch(testDirA.resolve("A-file2.java"));
        Path testDirAFile3 = touch(testDirA.resolve("A-file3.java"));
        final Path testDirAFile4 = touch(testDirA.resolve("A-file4.java"));
        Path testDirAFile5 = touch(testDirA.resolve("A-file5.java"));

        checkAndNotify();
        checkCollectionSizes("B", 1, 0, 0, 5, 0, 0);
        assertTrue(listener.getCreatedFiles().contains(testDirAFile1), "B testDirAFile1");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile2), "B testDirAFile2");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile3), "B testDirAFile3");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile4), "B testDirAFile4");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile5), "B testDirAFile5");

        assertTrue(Files.exists(testDirAFile1), "B testDirAFile1 exists");
        assertTrue(Files.exists(testDirAFile2), "B testDirAFile2 exists");
        assertTrue(Files.exists(testDirAFile3), "B testDirAFile3 exists");
        assertTrue(Files.exists(testDirAFile4), "B testDirAFile4 exists");
        assertTrue(Files.exists(testDirAFile5), "B testDirAFile5 exists");

        checkAndNotify();
        checkCollectionsEmpty("C");

        // Update first entry
        testDirAFile1 = touch(testDirAFile1);
        testDirA = touch(testDirA);
        checkAndNotify();
        checkCollectionSizes("D", 0, 1, 0, 0, 1, 0);
        assertTrue(listener.getChangedFiles().contains(testDirAFile1), "D testDirAFile1");

        // Update file with name between 2 entries
        testDirAFile3 = touch(testDirAFile3);
        testDirA = touch(testDirA);
        checkAndNotify();
        checkCollectionSizes("E", 0, 1, 0, 0, 1, 0);
        assertTrue(listener.getChangedFiles().contains(testDirAFile3), "E testDirAFile3");

        // Update last entry
        testDirAFile5 = touch(testDirAFile5);
        testDirA = touch(testDirA);
        checkAndNotify();
        checkCollectionSizes("F", 0, 1, 0, 0, 1, 0);
        assertTrue(listener.getChangedFiles().contains(testDirAFile5), "F testDirAFile5");
    }

    /**
     * Test checkAndNotify() deleting
     *
     * @throws Exception
     */
    @Test
    public void testFileDelete() throws Exception {
        checkAndNotify();
        checkCollectionsEmpty("A");
        Path testDirA = testDir.resolve("test-dir-A");
        Files.createDirectory(testDirA);
        touch(testDir);
        testDirA = touch(testDirA);
        final Path testDirAFile1 = touch(testDirA.resolve("A-file1.java"));
        final Path testDirAFile2 = touch(testDirA.resolve("A-file2.java"));
        final Path testDirAFile3 = touch(testDirA.resolve("A-file3.java"));
        final Path testDirAFile4 = touch(testDirA.resolve("A-file4.java"));
        final Path testDirAFile5 = touch(testDirA.resolve("A-file5.java"));

        assertTrue(Files.exists(testDirAFile1), "B testDirAFile1 exists");
        assertTrue(Files.exists(testDirAFile2), "B testDirAFile2 exists");
        assertTrue(Files.exists(testDirAFile3), "B testDirAFile3 exists");
        assertTrue(Files.exists(testDirAFile4), "B testDirAFile4 exists");
        assertTrue(Files.exists(testDirAFile5), "B testDirAFile5 exists");

        checkAndNotify();
        checkCollectionSizes("B", 1, 0, 0, 5, 0, 0);
        assertTrue(listener.getCreatedFiles().contains(testDirAFile1), "B testDirAFile1");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile2), "B testDirAFile2");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile3), "B testDirAFile3");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile4), "B testDirAFile4");
        assertTrue(listener.getCreatedFiles().contains(testDirAFile5), "B testDirAFile5");

        checkAndNotify();
        checkCollectionsEmpty("C");

        // Delete first entry
        deleteDirectoryRecursion(testDirAFile1);
        testDirA = touch(testDirA);
        checkAndNotify();
        checkCollectionSizes("D", 0, 1, 0, 0, 0, 1);
        assertFalse(Files.exists(testDirAFile1), "D testDirAFile1 exists");
        assertTrue(listener.getDeletedFiles().contains(testDirAFile1), "D testDirAFile1");

        // Delete file with name between 2 entries
        deleteDirectoryRecursion(testDirAFile3);
        testDirA = touch(testDirA);
        checkAndNotify();
        checkCollectionSizes("E", 0, 1, 0, 0, 0, 1);
        assertFalse(Files.exists(testDirAFile3), "E testDirAFile3 exists");
        assertTrue(listener.getDeletedFiles().contains(testDirAFile3), "E testDirAFile3");

        // Delete last entry
        deleteDirectoryRecursion(testDirAFile5);
        testDirA = touch(testDirA);
        checkAndNotify();
        checkCollectionSizes("F", 0, 1, 0, 0, 0, 1);
        assertFalse(Files.exists(testDirAFile5), "F testDirAFile5 exists");
        assertTrue(listener.getDeletedFiles().contains(testDirAFile5), "F testDirAFile5");
    }

    /**
     * Test checkAndNotify() method
     *
     * @throws Exception
     */
    @Test
    public void testObserveSingleFile() throws Exception {
        final Path testDirA = testDir.resolve("test-dir-A");
        final Path testDirAFile1 = testDirA.resolve("A-file1.java");
        Files.createDirectory(testDirA);

        final NIOFileFilter nameFilter = p -> p.toString().equals(testDirAFile1.toString());
        createObserver(testDirA, nameFilter);
        checkAndNotify();
        checkCollectionsEmpty("A");
        assertFalse(Files.exists(testDirAFile1), "A testDirAFile1 exists");

        // Create
        touch(testDirAFile1);
        Path testDirAFile2 = touch(testDirA.resolve("A-file2.txt"));  //filter should ignore
        Path testDirAFile3 = touch(testDirA.resolve("A-file3.java")); // filter should ignore
        assertTrue(Files.exists(testDirAFile1), "B testDirAFile1 exists");
        assertTrue(Files.exists(testDirAFile2), "B testDirAFile2 exists");
        assertTrue(Files.exists(testDirAFile3), "B testDirAFile3 exists");
        checkAndNotify();
        checkCollectionSizes("C", 0, 0, 0, 1, 0, 0);
        assertTrue(listener.getCreatedFiles().contains(testDirAFile1), "C created");
        assertFalse(listener.getCreatedFiles().contains(testDirAFile2), "C created");
        assertFalse(listener.getCreatedFiles().contains(testDirAFile3), "C created");

        // Modify
        touch(testDirAFile1);
        testDirAFile2 = touch(testDirAFile2);
        testDirAFile3 = touch(testDirAFile3);
        checkAndNotify();
        checkCollectionSizes("D", 0, 0, 0, 0, 1, 0);
        assertTrue(listener.getChangedFiles().contains(testDirAFile1), "D changed");
        assertFalse(listener.getChangedFiles().contains(testDirAFile2), "D changed");
        assertFalse(listener.getChangedFiles().contains(testDirAFile3), "D changed");

        // Delete
        deleteDirectoryRecursion(testDirAFile1);
        deleteDirectoryRecursion(testDirAFile2);
        deleteDirectoryRecursion(testDirAFile3);
        assertFalse(Files.exists(testDirAFile1), "E testDirAFile1 exists");
        assertFalse(Files.exists(testDirAFile2), "E testDirAFile2 exists");
        assertFalse(Files.exists(testDirAFile3), "E testDirAFile3 exists");
        checkAndNotify();
        checkCollectionSizes("E", 0, 0, 0, 0, 0, 1);
        assertTrue(listener.getDeletedFiles().contains(testDirAFile1), "E deleted");
        assertFalse(listener.getDeletedFiles().contains(testDirAFile2), "E deleted");
        assertFalse(listener.getDeletedFiles().contains(testDirAFile3), "E deleted");
    }

    /**
     * Call {@link NIOFileAlterationObserver#checkAndNotify()}.
     *
     * @throws Exception if an error occurs
     */
    protected void checkAndNotify() throws Exception {
        observer.checkAndNotify();
    }
}
