package jerectus.io;

import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jerectus.util.Sys;
import jerectus.util.Try;
import jerectus.util.function.TryConsumer;

public class IO {
    public static Path toPath(URI uri) {
        return uri == null ? null : Paths.get(uri);
    }

    public static Path toPath(URL url) {
        try {
            return url == null ? null : toPath(url.toURI());
        } catch (URISyntaxException e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public static String load(Path path) {
        return path == null ? null
                : !Files.exists(path) ? null : Try.get(() -> Files.readString(path, StandardCharsets.UTF_8));
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

    public static void save(Path path, TryConsumer<PrintWriter> fn) {
        Try.run(() -> {
            Files.createDirectories(path.getParent());
            try (var out = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
                fn.accept(out);
            }
        });
    }

    public static String getExtention(Path p) {
        return p.toString().replaceAll(".*(\\.[^/\\\\]+)", "$1");
    }
}