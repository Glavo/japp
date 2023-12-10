/*
 * Copyright (C) 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.packer.processor;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

final class PathListParser {
    static final char pathSeparatorChar = File.pathSeparatorChar;

    private final String list;
    private int offset = 0;

    Map<String, String> options;
    String path;

    PathListParser(String list) {
        this.list = list;
    }

    private void ensureNotAtEnd() {
        if (offset >= list.length()) {
            throw new IllegalArgumentException("Unexpected end of input");
        }
    }

    private void skipWhitespace() {
        while (offset < list.length() && list.charAt(offset) == ' ') {
            offset++;
        }
    }

    private boolean isLiteralChar(char ch) {
        switch (ch) {
            case '@':
            case '#':
            case '%':
            case '+':
            case '-':
            case '*':
            case '/':
            case '!':
                return true;
            default:
                return Character.isJavaIdentifierPart(ch);
        }
    }

    private String nextLiteral() {
        if (offset >= list.length()) {
            return "";
        }

        String res;
        char ch = list.charAt(offset);
        if (ch == '\'' || ch == '"') {
            int end = list.indexOf(ch, offset + 1);
            if (end < 0) {
                throw new IllegalArgumentException();
            }

            res = list.substring(offset + 1, end);
            offset = end + 1;
        } else if (isLiteralChar(ch)) {
            int end = offset + 1;
            while (end < list.length() && isLiteralChar(list.charAt(end))) {
                end++;
            }
            res = list.substring(offset, end);
            offset = end;
        } else {
            throw new IllegalArgumentException();
        }

        return res;
    }

    private void readPair() {
        String name = nextLiteral();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Option name cannot be empty");
        }

        skipWhitespace();
        ensureNotAtEnd();

        if (list.charAt(offset++) != '=') {
            throw new IllegalArgumentException();
        }

        skipWhitespace();
        String value = nextLiteral();

        if (options.put(name, value) != null) {
            throw new IllegalArgumentException("Duplicate option: " + name);
        }
    }

    boolean scanNext() {
        if (offset >= list.length()) {
            return false;
        }

        char ch = 0;
        while (offset < list.length() && (ch = list.charAt(offset)) == pathSeparatorChar) {
            offset++;
        }

        if (offset >= list.length()) {
            return false;
        }

        options = new LinkedHashMap<>();
        if (ch == '[') {
            offset++;
            skipWhitespace();
            ensureNotAtEnd();

            boolean isFirst = true;
            while ((ch = list.charAt(offset)) != ']') {
                if (isFirst) {
                    isFirst = false;
                } else {
                    if (ch != ',') {
                        throw new IllegalArgumentException();
                    }
                    offset++;
                    skipWhitespace();
                    ensureNotAtEnd();
                }

                readPair();
                skipWhitespace();
                ensureNotAtEnd();
            }

            offset++; // skip ']'
        }

        int end = list.indexOf(pathSeparatorChar, offset);
        if (end < 0) {
            path = list.substring(offset);
            offset = list.length();
        } else {
            path = list.substring(offset, end);
            offset = end + 1;
        }

        return true;
    }
}
