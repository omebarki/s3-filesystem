package omar.mebarki;

import com.google.common.collect.ImmutableMap;
import com.upplication.s3fs.AmazonS3Factory;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.*;
import java.text.MessageFormat;
import java.util.Map;

public class AppMain {
    public static void main(String[] args) throws Exception {
        String s3AccessKey = "minioadmin";
        String s3SecretKey = "minioadmin";
        String s3Server = "192.168.160.129:9000"; // example server name
        String s3BucketName = "omar"; // example bucket name
        Map<String, ?> env = ImmutableMap.<String, Object>builder()
                .put(AmazonS3Factory.PROTOCOL, "HTTP")
                .build();

        URI uri = URI.create(MessageFormat.format("s3://{0}:{1}@{2}", s3AccessKey, s3SecretKey, s3Server));
        FileSystem s3fs = FileSystems.newFileSystem(uri, env);
        Path bucketPath = s3fs.getPath("/" + s3BucketName);

        Files.list(bucketPath).
                forEach(path -> {
                    System.out.println("-------------------------------------------");
                    System.out.println("File Name:" + path.getFileName());
                    try {
                        Path target = Paths.get(URLDecoder.decode("target/" + path.getFileName(), "UTF-8"));
                        if (!Files.isDirectory(path)) {
                            System.out.println("copy to: " + target.getFileName());
                            Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("deleted");
                            Files.delete(path);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("-------------------------------------------");
                });
    }
}
