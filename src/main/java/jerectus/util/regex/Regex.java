package jerectus.util.regex;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex {
    public static String replace(String text, Pattern ptn, Function<Matcher, String> fn) {
        StringBuilder sb = new StringBuilder();
        Matcher m = ptn.matcher(text);
        int i = 0;
        while (m.find()) {
            sb.append(text.substring(i, m.start()));
            sb.append(fn.apply(m));
            i = m.end();
        }
        sb.append(text.substring(i));
        return sb.toString();
    }

    public static String replace(String text, String ptn, Function<Matcher, String> fn) {
        return replace(text, Pattern.compile(ptn), fn);
    }
}