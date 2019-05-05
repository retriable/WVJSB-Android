//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.retriable.wvjsb;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class StringEscapeUtils {

    private StringEscapeUtils() {
    }

    public static String unescapeJavaScript(String str) {
        return unescapeJava(str);
    }

    public static void unescapeJavaScript(Writer out, String str) throws IOException {
        unescapeJava(out, str);
    }

    public static String unescapeJava(String str) {
        if (str == null) {
            return null;
        } else {
            try {
                StringWriter writer = new StringWriter(str.length());
                unescapeJava(writer, str);
                return writer.toString();
            } catch (IOException var2) {
                throw new RuntimeException(var2);
            }
        }
    }

    public static void unescapeJava(Writer out, String str) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("The Writer must not be null");
        } else if (str != null) {
            int sz = str.length();
            StringBuffer unicode = new StringBuffer(4);
            boolean hadSlash = false;
            boolean inUnicode = false;

            for (int i = 0; i < sz; ++i) {
                char ch = str.charAt(i);
                if (inUnicode) {
                    unicode.append(ch);
                    if (unicode.length() == 4) {
                        try {
                            int value = Integer.parseInt(unicode.toString(), 16);
                            out.write((char) value);
                            unicode.setLength(0);
                            inUnicode = false;
                            hadSlash = false;
                        } catch (NumberFormatException var9) {
                            throw new RuntimeException("Unable to parse unicode value: " + unicode, var9);
                        }
                    }
                } else if (hadSlash) {
                    hadSlash = false;
                    switch (ch) {
                        case '"':
                            out.write(34);
                            break;
                        case '\'':
                            out.write(39);
                            break;
                        case '\\':
                            out.write(92);
                            break;
                        case 'b':
                            out.write(8);
                            break;
                        case 'f':
                            out.write(12);
                            break;
                        case 'n':
                            out.write(10);
                            break;
                        case 'r':
                            out.write(13);
                            break;
                        case 't':
                            out.write(9);
                            break;
                        case 'u':
                            inUnicode = true;
                            break;
                        default:
                            out.write(ch);
                    }
                } else if (ch == '\\') {
                    hadSlash = true;
                } else {
                    out.write(ch);
                }
            }

            if (hadSlash) {
                out.write(92);
            }

        }
    }


}
