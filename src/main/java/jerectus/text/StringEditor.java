package jerectus.text;

public class StringEditor {
    private StringBuilder sb;

    public StringEditor(StringBuilder sb) {
        this.sb = sb;
    }

    public StringEditor() {
        this(new StringBuilder());
    }

    public void append(Object... values) {
        for (var value : values) {
            sb.append(value);
        }
    }
}