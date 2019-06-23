package jerectus.util.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

/**
 * @see http://etc9.hatenablog.com/entry/2017/02/24/070608
 */
public class LogFormatter extends Formatter {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSxxxx");

    private static final Map<Level, String> levelMsgMap = Collections.unmodifiableMap(new HashMap<Level, String>() {
        private static final long serialVersionUID = 1L;

        {
            put(Level.SEVERE, "ERROR");
            put(Level.WARNING, "WARN");
            put(Level.INFO, "INFO");
            put(Level.CONFIG, "INFO");
            put(Level.FINE, "DEBUG");
            put(Level.FINER, "DEBUG");
            put(Level.FINEST, "DEBUG");
        }
    });

    private AtomicInteger nameColumnWidth = new AtomicInteger(16);

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder(200);

        sb.append(formatter.format(OffsetDateTime.now()));
        sb.append(" ");
        sb.append(levelMsgMap.get(record.getLevel()));
        sb.append(" ");
        int width = nameColumnWidth.intValue();
        String category = adjustLength(record.getLoggerName(), width);
        sb.append("[");
        sb.append(category);
        sb.append("] ");

        if (category.length() > width) {
            // grow in length.
            nameColumnWidth.compareAndSet(width, category.length());
        }

        sb.append(formatMessage(record));
        sb.append(System.lineSeparator());
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
            }
        }

        return sb.toString();
    }

    static String adjustLength(String packageName, int aimLength) {

        int overflowWidth = packageName.length() - aimLength;

        String[] fragment = packageName.split(Pattern.quote("."));
        for (int i = 0; i < fragment.length - 1; i++) {
            if (fragment[i].length() > 1 && overflowWidth > 0) {

                int cutting = (fragment[i].length() - 1) - overflowWidth;
                cutting = (cutting < 0) ? (fragment[i].length() - 1) : overflowWidth;

                fragment[i] = fragment[i].substring(0, fragment[i].length() - cutting);
                overflowWidth -= cutting;
            }
        }

        String result = String.join(".", fragment);
        while (result.length() < aimLength) {
            result += " ";
        }

        return result;
    }
}