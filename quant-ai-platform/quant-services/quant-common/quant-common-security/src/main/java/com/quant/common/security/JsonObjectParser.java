package com.quant.common.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonObjectParser {

    static Object parse(String json) {
        return new Parser(json).parse();
    }

    private static final class Parser {
        private final String json;
        private int index;

        private Parser(String json) {
            this.json = json == null ? "" : json;
        }

        private Object parse() {
            Object value = parseValue();
            skipWhitespace();
            if (index != json.length()) {
                throw new IllegalArgumentException("Unexpected trailing JSON content");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= json.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            char current = json.charAt(index);
            return switch (current) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                result.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return result;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < json.length()) {
                char current = json.charAt(index++);
                if (current == '"') {
                    return result.toString();
                }
                if (current == '\\') {
                    if (index >= json.length()) {
                        throw new IllegalArgumentException("Invalid JSON escape");
                    }
                    char escaped = json.charAt(index++);
                    switch (escaped) {
                        case '"' -> result.append('"');
                        case '\\' -> result.append('\\');
                        case '/' -> result.append('/');
                        case 'b' -> result.append('\b');
                        case 'f' -> result.append('\f');
                        case 'n' -> result.append('\n');
                        case 'r' -> result.append('\r');
                        case 't' -> result.append('\t');
                        case 'u' -> result.append(parseUnicode());
                        default -> throw new IllegalArgumentException("Invalid JSON escape");
                    }
                } else {
                    result.append(current);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private char parseUnicode() {
            if (index + 4 > json.length()) {
                throw new IllegalArgumentException("Invalid JSON unicode escape");
            }
            String hex = json.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid JSON unicode escape", ex);
            }
        }

        private Object parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            readDigits();
            boolean decimal = false;
            if (peek('.')) {
                decimal = true;
                index++;
                readDigits();
            }
            if (peek('e') || peek('E')) {
                decimal = true;
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                readDigits();
            }
            if (start == index) {
                throw new IllegalArgumentException("Invalid JSON number");
            }
            String number = json.substring(start, index);
            try {
                return decimal ? Double.parseDouble(number) : Long.parseLong(number);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid JSON number", ex);
            }
        }

        private void readDigits() {
            int start = index;
            while (index < json.length() && Character.isDigit(json.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw new IllegalArgumentException("Expected JSON digit");
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!json.startsWith(literal, index)) {
                throw new IllegalArgumentException("Invalid JSON literal");
            }
            index += literal.length();
            return value;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= json.length() || json.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected JSON character " + expected);
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < json.length() && json.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }
    }

    private JsonObjectParser() {
    }
}
