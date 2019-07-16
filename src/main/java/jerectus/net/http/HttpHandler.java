package jerectus.net.http;

import java.util.HashMap;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import jerectus.html.template.HtmlTemplate;
import jerectus.net.http.RequestModelMapper.MappingException;
import jerectus.util.Reflect;
import jerectus.util.Resources;
import jerectus.util.Sys;
import jerectus.util.logging.Logger;
import jerectus.util.regex.PatternMatcher;
import jerectus.util.template.TemplateContext;
import jerectus.validation.ValidateException;
import jerectus.validation.Validator;

public class HttpHandler extends org.glassfish.grizzly.http.server.HttpHandler {
    static {
        Logger.setup("jerectus", Logger.DEBUG);
    }

    private String rootPackage;
    private String rootPath;

    {
        var props = Sys.loadProperties(getClass(), "app");
        rootPackage = props.getProperty("root.package", getClass().getPackageName());
        rootPath = "/" + rootPackage.replace('.', '/');
    }

    @Override
    public void service(Request request, Response response) throws Exception {
        var path = rootPath + request.getRequestURI();
        if (path.endsWith("/")) {
            path += "index.html";
        }
        var pm = new PatternMatcher();
        if (pm.matches(path, "/(.*)/([^/]+)\\.html")) {
            var type = Class.forName(pm.group(1).replace('/', '.') + "." + Sys.capitalize(pm.group(2)));
            var options = new HashMap<String, String>();
            Object model = null;
            Throwable ex = null;
            try {
                model = RequestModelMapper.convert(request, options);
                Object bean = RequestModelMapper.convert(model, type);
                Validator.getInstance().validate(bean);
                if (options.containsKey("-submit")) {
                    Reflect.invoke(bean, options.get("-submit"));
                }
                model = bean;
            } catch (MappingException e) {
                ex = e.getCause();
                model = e.getValues();
            } catch (ValidateException e) {
                ex = e;
            }
            if (ex != null) {
                var ctx = new TemplateContext(model);
                ctx.set("__error__", "<div style=\"color: red;\">error: " + ex + "</div>");
                model = ctx;
            }
            var tmpl = new HtmlTemplate(Resources.get(this, path));
            tmpl.render(response.getWriter(), model);
        } else {
            response.getWriter().write("unknown type: " + path);
        }
    }
}