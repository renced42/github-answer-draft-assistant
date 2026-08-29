package hu.gov.nav.answerdraft;

/** A Workspace Agent kis JSON-válaszaihoz szükséges minimális JSON-kezelés. */
public final class Json {
    private Json() {
    }

    /** JSON string literállá alakít egy Java szöveget. */
    public static String quote(String value) {
        StringBuilder result = new StringBuilder(value.length() + 16).append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) {
                        result.append("\\u%04x".formatted((int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.append('"').toString();
    }

    /** Kikeresi egy felső szintű JSON string mező értékét. */
    public static String stringField(String json, String field) {
        String marker = quote(field);
        int position = json.indexOf(marker);
        if (position < 0) {
            return null;
        }
        position = json.indexOf(':', position + marker.length());
        if (position < 0) {
            return null;
        }
        position++;
        while (position < json.length() && Character.isWhitespace(json.charAt(position))) {
            position++;
        }
        if (position >= json.length() || json.charAt(position) != '"') {
            return null;
        }
        return readString(json, position + 1);
    }

    private static String readString(String json, int position) {
        StringBuilder result = new StringBuilder();
        while (position < json.length()) {
            char character = json.charAt(position++);
            if (character == '"') {
                return result.toString();
            }
            if (character != '\\') {
                result.append(character);
                continue;
            }
            if (position >= json.length()) {
                break;
            }
            char escaped = json.charAt(position++);
            switch (escaped) {
                case '"', '\\', '/' -> result.append(escaped);
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> {
                    if (position + 4 > json.length()) {
                        throw new IllegalArgumentException("Hibás Unicode escape a JSON-válaszban.");
                    }
                    result.append((char) Integer.parseInt(json.substring(position, position + 4), 16));
                    position += 4;
                }
                default -> throw new IllegalArgumentException("Ismeretlen JSON escape: " + escaped);
            }
        }
        throw new IllegalArgumentException("Lezáratlan JSON string.");
    }
}
