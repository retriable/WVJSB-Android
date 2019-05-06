package com.retriable.wvjsb;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringWriter;

final class StringUtils {

    @SuppressWarnings("unused")
    @Contract("null -> fail")
    @NotNull
    static String unescape(@Nullable String s) throws Throwable{
        if (null == s){
            throw new Throwable("string must not be null");
        }
        int len = s.length();
        StringWriter writer = new StringWriter(len);
        StringBuilder builder = new StringBuilder(4);
        boolean slash = false;
        boolean unicode = false;
        for (int i = 0; i < len ;i++) {
            char c = s.charAt(i);
            if (unicode) {
                builder.append(c);
                //unicode is four chars
                if (builder.length() == 4) {
                    int value = Integer.parseInt(builder.toString(), 16);
                    writer.write((char) value);
                    builder.setLength(0);
                    unicode = false;
                }
            } else if (slash) {
                slash = false;
                switch (c) {
                    case '\\':
                        writer.write("\\");
                        break;
                    case '\'':
                        writer.write('\'');
                        break;
                    case '"':
                        writer.write('"');
                        break;
                    case 'b':
                        writer.write('\b');
                        break;
                    case 'f':
                        writer.write('\f');
                        break;
                    case 'n':
                        writer.write('\n');
                        break;
                    case 'r':
                        writer.write('\r');
                        break;
                    case 't':
                        writer.write('\t');
                        break;
                    case 'u':
                        unicode = true;
                        break;
                    default:
                        writer.write(c);
                }
            } else if (c == '\\') {
                slash = true;
            } else {
                writer.write(c);
            }
        }
        if (slash) {
            //last character is a slash,this is a issue of escaping
            writer.write('\\');
        }
        return writer.toString();
    }

    @Contract("null -> fail")
    @NotNull
    static String escape(@Nullable String s) throws Throwable{
        if (null == s){
            throw new Throwable("string must not be null");
        }
        int len = s.length();
        StringBuilder builder = new StringBuilder(len);
        for (int i=0;i<len;i++){
            char c = s.charAt(i);
            switch (c){
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\'':
                    builder.append("\\'");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                    default:
                        builder.append(c);
                        break;
            }
        }
        s = builder.toString();
        s = s.replace("\u2028","\\u2028");
        s = s.replace("\u2029","\\u2029");
        return s;
    }
}
