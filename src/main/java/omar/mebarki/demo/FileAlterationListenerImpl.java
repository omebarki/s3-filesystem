package omar.mebarki.demo;

import omar.mebarki.monitor.NIOFileAlterationListener;
import omar.mebarki.monitor.NIOFileAlterationObserver;

import java.nio.file.Path;

/**
 * @author ashraf
 */
public class FileAlterationListenerImpl implements NIOFileAlterationListener {


    @Override
    public void onStart(final NIOFileAlterationObserver observer) {
        System.out.println("The FileListener has started on "
                + observer.getDirectory());
    }

    @Override
    public void onDirectoryCreate(final Path directory) {
        System.out.println(directory + " was created.");
    }

    @Override
    public void onDirectoryChange(final Path directory) {
        System.out.println(directory + " wa modified");
    }

    @Override
    public void onDirectoryDelete(final Path directory) {
        System.out.println(directory + " was deleted.");
    }

    @Override
    public void onFileCreate(final Path file) {
        System.out.println(file + " was created.");
        /*System.out.println("1. length: " + file.length());
        System.out
                .println("2. last modified: " + new Date(file.lastModified()));
        System.out.println("3. readable: " + file.canRead());
        System.out.println("4. writable: " + file.canWrite());
        System.out.println("5. executable: " + file.canExecute());*/
    }

    @Override
    public void onFileChange(final Path file) {
        System.out.println(file + " was modified.");
        /*System.out.println("1. length: " + file.length());
        System.out
                .println("2. last modified: " + new Date(file.lastModified()));
        System.out.println("3. readable: " + file.canRead());
        System.out.println("4. writable: " + file.canWrite());
        System.out.println("5. executable: " + file.canExecute());*/
    }

    @Override
    public void onFileDelete(final Path file) {
        System.out.println(file + " was deleted.");
    }

    @Override
    public void onStop(final NIOFileAlterationObserver observer) {
        System.out.println("The FileListener has stopped on "
                + observer.getDirectory());
    }
}