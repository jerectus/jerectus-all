package jerectus.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternMatcher {
    private Matcher m;

    public boolean matches(String text, String pattern) {
        m = Pattern.compile(pattern).matcher(text);
        return m.matches();
    }

    public String group() {
        return m.group();
    }

    public String group(int index) {
        return m.group(index);
    }
}
