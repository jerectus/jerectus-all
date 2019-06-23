package jerectus.util;

public class StringEditor implements CharSequence {
    private StringBuilder sb;

    public StringEditor(StringBuilder sb) {
        this.sb = sb;
    }

    public StringEditor() {
        this(new StringBuilder());
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    @Override
    public int length() {
        return sb.length();
    }

    @Override
    public char charAt(int index) {
        return sb.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return sb.subSequence(start, end);
    }

    public StringEditor append(Object... values) {
        for (var value : values) {
            sb.append(value);
        }
        return this;
    }
}
