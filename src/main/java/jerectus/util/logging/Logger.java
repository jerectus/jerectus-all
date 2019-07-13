package jerectus.util.logging;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;

public class Logger {
    public static final Level ERROR = Level.SEVERE;
    public static final Level WARN = Level.WARNING;
    public static final Level INFO = Level.INFO;
    public static final Level DEBUG = Level.FINE;
    private static final Map<String, Logger> loggers = new ConcurrentHashMap<>();

    private java.util.logging.Logger logger;

    protected Logger(String name) {
        logger = java.util.logging.Logger.getLogger(name);
    }

    public Level getLevel() {
        return logger.getLevel();
    }

    public void setLevel(Level level) {
        logger.setLevel(level);
    }

    public void log(Level level, Object... values) {
        if (logger.isLoggable(level)) {
            var sb = new StringBuilder();
            Throwable t = null;
            for (int i = 0; i < values.length; i++) {
                if (i == values.length - 1 && values[i] instanceof Throwable) {
                    t = (Throwable) values[i];
                } else {
                    sb.append(values[i]);
                }
            }
            var s = sb.toString();
            if (s.indexOf("\n") != -1) {
                s = s.replace("\n", "\n\t");
            }
            logger.log(level, s, t);
        }
    }

    public void debug(Object... values) {
        log(DEBUG, values);
    }

    public void info(Object... values) {
        log(INFO, values);
    }

    public void warn(Object... values) {
        log(WARN, values);
    }

    public void error(Object... values) {
        log(ERROR, values);
    }

    public static Logger getLogger(String name) {
        return loggers.computeIfAbsent(name, _0 -> new Logger(name));
    }

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    public static void setup(String name, Level level) {
        try {
            var logger = java.util.logging.Logger.getLogger(name);
            logger.setUseParentHandlers(false);

            for (var h : logger.getHandlers()) {
                if (h instanceof FileHandler) {
                    ((FileHandler) h).close();
                }
                logger.removeHandler(h);
            }

            var f = new LogFormatter();
            {
                var h = new FileHandler("./logs/app.%g.%u.log", 50000, 2, true);
                h.setEncoding("UTF-8");
                h.setFormatter(f);
                h.setLevel(Level.ALL);
                logger.addHandler(h);
            }
            {
                var h = new ConsoleHandler();
                h.setEncoding("UTF-8");
                h.setFormatter(f);
                h.setLevel(Level.WARNING);
                logger.addHandler(h);
            }

            logger.setLevel(level);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }
}
