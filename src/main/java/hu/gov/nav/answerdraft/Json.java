package hu.gov.nav.answerdraft;

import java.util.*;

final class Json {
    private Json() {}

    static Object parse(String text) { return new Parser(text).value(); }

    static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + escape(s) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringJoiner out = new StringJoiner(",", "{", "}");
            map.forEach((k, v) -> out.add(stringify(String.valueOf(k)) + ":" + stringify(v)));
            return out.toString();
        }
        if (value instanceof Iterable<?> values) {
            StringJoiner out = new StringJoiner(",", "[", "]");
            values.forEach(v -> out.add(stringify(v)));
            return out.toString();
        }
        return stringify(value.toString());
    }

    @SuppressWarnings("unchecked") static Map<String,Object> object(Object value) { return (Map<String,Object>) value; }
    @SuppressWarnings("unchecked") static List<Object> array(Object value) { return value == null ? List.of() : (List<Object>) value; }
    static String string(Object value) { return value == null ? "" : String.valueOf(value); }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder();
        for (char c : s.toCharArray()) switch (c) {
            case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\");
            case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
            default -> { if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
        }
        return out.toString();
    }

    private static final class Parser {
        private final String text; private int p;
        Parser(String text) { this.text = text; }
        Object value() {
            ws(); if (p >= text.length()) throw new IllegalArgumentException("Üres JSON");
            char c = text.charAt(p);
            Object v = switch (c) {
                case '{' -> object(); case '[' -> array(); case '"' -> string();
                case 't' -> literal("true", true); case 'f' -> literal("false", false); case 'n' -> literal("null", null);
                default -> number();
            };
            ws(); return v;
        }
        private Map<String,Object> object() {
            Map<String,Object> out = new LinkedHashMap<>(); p++; ws();
            if (take('}')) return out;
            do { ws(); String key = string(); ws(); require(':'); out.put(key, value()); ws(); } while (take(','));
            require('}'); return out;
        }
        private List<Object> array() {
            List<Object> out = new ArrayList<>(); p++; ws(); if (take(']')) return out;
            do { out.add(value()); ws(); } while (take(',')); require(']'); return out;
        }
        private String string() {
            require('"'); StringBuilder out = new StringBuilder();
            while (p < text.length()) { char c = text.charAt(p++); if (c == '"') return out.toString();
                if (c != '\\') out.append(c); else { char e = text.charAt(p++); switch (e) {
                    case '"','\\','/' -> out.append(e); case 'b' -> out.append('\b'); case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n'); case 'r' -> out.append('\r'); case 't' -> out.append('\t');
                    case 'u' -> { out.append((char)Integer.parseInt(text.substring(p,p+4),16)); p += 4; }
                    default -> throw new IllegalArgumentException("Hibás JSON escape");
                }}
            } throw new IllegalArgumentException("Lezáratlan JSON string");
        }
        private Object number() {
            int start = p; while (p < text.length() && "-+0123456789.eE".indexOf(text.charAt(p)) >= 0) p++;
            String n = text.substring(start,p); return n.contains(".") || n.contains("e") || n.contains("E") ? Double.valueOf(n) : Long.valueOf(n);
        }
        private Object literal(String token,Object value) { if (!text.startsWith(token,p)) throw new IllegalArgumentException("Hibás JSON"); p += token.length(); return value; }
        private void ws() { while (p < text.length() && Character.isWhitespace(text.charAt(p))) p++; }
        private boolean take(char c) { if (p < text.length() && text.charAt(p)==c) { p++; return true; } return false; }
        private void require(char c) { ws(); if (!take(c)) throw new IllegalArgumentException("Várt karakter: " + c); }
    }
}
