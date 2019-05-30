package jerectus.util.logging;

import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jerectus.util.Sys;

public class Logger {
    public static enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    private static final Map<String, Logger> loggers = new ConcurrentHashMap<>();
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");

    private String name;
    private Level level = Level.INFO;
    private PrintWriter out;

    public Logger(String name) {
        this.name = name;
        out = new PrintWriter(System.out, true);
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void log(Level level, Object... values) {
        if (level.compareTo(this.level) >= 0) {
            out.print(dateTimeFormatter.format(ZonedDateTime.now()));
            out.print(" ");
            out.print(level);
            out.print(" ");
            out.print(name);
            for (var value : values) {
                out.print(" ");
                if (value == null) {
                    out.print("null");
                } else if (value.getClass().isArray()) {
                    out.print("[");
                    var sep = "";
                    for (var elem : Sys.each(value)) {
                        out.print(sep);
                        out.print(elem);
                        sep = ", ";
                    }
                    out.print("]");
                } else {
                    out.print(value);
                }
            }
            out.println();
        }
    }

    public void debug(Object... values) {
        log(Level.DEBUG, values);
    }

    public void info(Object... values) {
        log(Level.INFO, values);
    }

    public void warn(Object... values) {
        log(Level.WARN, values);
    }

    public void error(Object... values) {
        log(Level.ERROR, values);
    }

    public static Logger getLogger(String name) {
        return loggers.computeIfAbsent(name, _0 -> new Logger(name));
    }

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }
}
