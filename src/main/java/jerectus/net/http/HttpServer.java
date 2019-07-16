package jerectus.net.http;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import jerectus.html.template.HtmlTemplate;
import jerectus.util.Resources;
import jerectus.util.Sys;
import jerectus.util.Try;
import jerectus.util.regex.PatternMatcher;

public class HttpServer {
    private org.glassfish.grizzly.http.server.HttpServer server;

    public void start(Class<?> rootClass) {
        var rootPackage = getRootPackage(rootClass);
        var rootPath = "/" + rootPackage.replace('.', '/');

        server = org.glassfish.grizzly.http.server.HttpServer.createSimpleServer(null, 80);
        var cfg = server.getServerConfiguration();
        cfg.addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                var path = rootPath + request.getRequestURI();
                if (path.endsWith("/")) {
                    path += "index.html";
                }
                var pm = new PatternMatcher();
                if (pm.matches(path, "/(.*)/([^/]+)\\.html")) {
                    var type = Class.forName(pm.group(1).replace('/', '.') + "." + Sys.capitalize(pm.group(2)));
                    var model = RequestModelMapper.convert(request, type, null);
                    var tmpl = new HtmlTemplate(Resources.get(this, path));
                    tmpl.render(response.getWriter(), model);
                } else {
                    response.getWriter().write("unknown type: " + path);
                }
            }
        });
        Try.run(() -> {
            server.start();
            Thread.currentThread().join();
        });
    }

    private String getRootPackage(Class<?> rootClass) {
        if (rootClass != null) {
            return rootClass.getPackage().getName();
        }
        var props = Sys.loadProperties(null, "webapp");
        return props.getProperty("root.package");
    }

    public static void main(String... args) {
        new HttpServer().start(null);
    }
}