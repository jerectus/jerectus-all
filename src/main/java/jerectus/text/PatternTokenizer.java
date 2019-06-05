package jerectus.text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jerectus.util.YieldIterator;

public class PatternTokenizer {
    private List<String> types = new ArrayList<>();
    private StringBuilder patternBuilder = new StringBuilder();

    public void addTokenPattern(String type, String pattern) {
        types.add(type);
        if (types.size() > 1) {
            patternBuilder.append("|");
        }
        patternBuilder.append("(?<");
        patternBuilder.append(type);
        patternBuilder.append(">");
        patternBuilder.append(pattern);
        patternBuilder.append(")");
    }

    public Iterable<Token> parse(String text) {
        return () -> new YieldIterator<Token>() {
            Pattern pattern = Pattern.compile(patternBuilder.toString());
            Matcher m = pattern.matcher(text);
            int pos = 0;

            @Override
            public void generate() {
                if (m.find()) {
                    if (m.start() > pos) {
                        yield(new Token("", text.substring(pos, m.start())));
                    }
                    for (var type : types) {
                        var value = m.group(type);
                        if (value != null) {
                            yield(new Token(type, value));
                            break;
                        }
                    }
                    pos = m.end();
                }
            }
        };
    }

    public static class Token {
        public String type;
        public String value;

        private Map<String, Object> attrMap;

        public Token(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public boolean is(String type) {
            return this.type.matches(type);
        }

        public boolean matches(String pattern) {
            return value.matches(pattern);
        }

        public Object attr(String name) {
            return attrMap == null ? null : attrMap.get(name);
        }

        public void attr(String name, Object value) {
            if (attrMap == null) {
                attrMap = new LinkedHashMap<>();
            }
            attrMap.put(name, value);
        }
    }
}
