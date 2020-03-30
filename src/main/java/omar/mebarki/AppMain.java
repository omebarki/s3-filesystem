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
                        if (!Files.isDirectory(path)) {
                            //Telecharge le fichier de puis S3
                            Path downloadTarget = Paths.get(URLDecoder.decode("target/" + path.getFileName(), "UTF-8"));
                            System.out.println("Download to: " + downloadTarget.getFileName());
                            Files.copy(path, downloadTarget, StandardCopyOption.REPLACE_EXISTING);

                            //Deplace le fichier à l'intérieur de S3
                            Path s3MoveTarget = s3fs.getPath("/" + s3BucketName + "/" + URLDecoder.decode("target/" + path.getFileName(), "UTF-8"));
                            System.out.println("Move to: " + s3MoveTarget.getFileName());
                            Files.move(path, s3MoveTarget, StandardCopyOption.REPLACE_EXISTING);

                            //System.out.println("deleted");
                            //Files.delete(path);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("-------------------------------------------");
                });
    }
}
