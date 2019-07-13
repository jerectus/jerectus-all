package jerectus.net.http;

public class App {
    public static void main(String... args) {
        new HttpServer().start(App.class);
    }
}
