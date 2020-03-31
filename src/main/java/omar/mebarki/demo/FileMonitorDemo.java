package omar.mebarki.demo;

import com.google.common.collect.ImmutableMap;
import com.upplication.s3fs.AmazonS3Factory;
import omar.mebarki.monitor.S3FileAlterationMonitor;
import omar.mebarki.monitor.S3FileAlterationObserver;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Map;

public class FileMonitorDemo {

    // The monitor will perform polling on the folder every 30 seconds
    private static final long pollingInterval = 10 * 1000;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        String s3AccessKey = "newAccessKey";
        String s3SecretKey = "newSecretKey";
        String s3Server = "192.168.160.129:8000"; // example server name
        String s3BucketName = "omar"; // example bucket name
        Map<String, ?> env = ImmutableMap.<String, Object>builder()
                .put(AmazonS3Factory.PROTOCOL, "HTTP")
                .build();

        URI uri = URI.create(MessageFormat.format("s3://{0}:{1}@{2}", s3AccessKey, s3SecretKey, s3Server));
        FileSystem s3fs = FileSystems.newFileSystem(uri, env);
        Path bucketPath = s3fs.getPath("/" + s3BucketName);

        // Change this to match the environment you want to watch.
        final Path directory = bucketPath;

        // Create a new FileAlterationObserver on the given directory
        S3FileAlterationObserver fao = new S3FileAlterationObserver(directory);

        // Create a new FileAlterationListenerImpl and pass it the previously created FileAlterationObserver
        fao.addListener(new FileAlterationListenerImpl());

        // Create a new FileAlterationMonitor with the given pollingInterval period
        final S3FileAlterationMonitor monitor = new S3FileAlterationMonitor(
                pollingInterval);

        // Add the previously created FileAlterationObserver to FileAlterationMonitor
        monitor.addObserver(fao);

        // Start the FileAlterationMonitor
        monitor.start();

        System.out.println("Starting monitor (" + directory
                + "). \"Press CTRL+C to stop\"");
    }
}
