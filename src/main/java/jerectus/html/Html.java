package jerectus.html;

import jerectus.util.Sys;

public class Html {
    public static String escape0(Object value) {
        if (value instanceof String) {
            return "\"" + ((String) value).replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r")
                    .replace("\n", "\\n") + "\"";
        }
        return Sys.toString(value);
    }
}