package jerectus.io;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jerectus.util.Try;

public class IO {
    public static String load(Path path) {
        return !Files.exists(path) ? null : Try.get(() -> Files.readString(path, StandardCharsets.UTF_8));
    }

    public static String load(String path) {
        return load(Paths.get(path));
    }

    public static void save(Path path, CharSequence content) {
        Try.run(() -> {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        });
    }

    public static void save(String path, CharSequence content) {
        save(Paths.get(path), content);
    }
}