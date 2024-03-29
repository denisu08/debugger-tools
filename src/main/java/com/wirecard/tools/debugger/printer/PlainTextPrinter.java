package com.wirecard.tools.debugger.printer;

import org.jd.core.v1.api.printer.Printer;

public class PlainTextPrinter implements Printer {
    protected static final String TAB = "  ";
    protected static final String NEWLINE = "\n";

    protected int indentationCount;
    protected StringBuilder sb = new StringBuilder();
    protected int realLineNumber = 0;
    protected String format;

    protected boolean escapeUnicodeCharacters;

    public PlainTextPrinter() {
        this.escapeUnicodeCharacters = false;
    }

    public PlainTextPrinter(boolean escapeUnicodeCharacters) {
        this.escapeUnicodeCharacters = escapeUnicodeCharacters;
    }

    public void init() {
        sb.setLength(0);
        realLineNumber = 0;
        indentationCount = 0;
    }

    public String toString() {
        return sb.toString();
    }

    // --- Printer --- //
    public void start(int maxLineNumber, int majorVersion, int minorVersion) {
        this.indentationCount = 0;

        if (maxLineNumber == 0) {
            format = "%4d";
        } else {
            int width = 2;

            while (maxLineNumber >= 10) {
                width++;
                maxLineNumber /= 10;
            }

            format = "%" + width + "d";
        }
    }

    public void end() {
    }

    public void printText(String text) {
        if (escapeUnicodeCharacters) {
            for (int i = 0, len = text.length(); i < len; i++) {
                char c = text.charAt(i);

                if (c < 128) {
                    sb.append(c);
                } else {
                    int h = (c >> 24);

                    sb.append("\\u");
                    sb.append((h <= 9) ? (h + '0') : (h + 'A'));
                    h = (c >> 16) & 255;
                    sb.append((h <= 9) ? (h + '0') : (h + 'A'));
                    h = (c >> 8) & 255;
                    sb.append((h <= 9) ? (h + '0') : (h + 'A'));
                    h = (c) & 255;
                    sb.append((h <= 9) ? (h + '0') : (h + 'A'));
                }
            }
        } else {
            sb.append(text);
        }
    }

    public void printNumericConstant(String constant) {
        sb.append(constant);
    }

    public void printStringConstant(String constant, String ownerInternalName) {
        printText(constant);
    }

    public void printKeyword(String keyword) {
        sb.append(keyword);
    }

    public void printDeclaration(int type, String internalTypeName, String name, String descriptor) {
        printText(name);
    }

    public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) {
        printText(name);
    }

    public void indent() {
        this.indentationCount++;
    }

    public void unindent() {
        if (this.indentationCount > 0)
            this.indentationCount--;
    }

    public void startLine(int lineNumber) {
        printLineNumber(lineNumber);

        for (int i = 0; i < indentationCount; i++)
            sb.append(TAB);
    }

    public void endLine() {
        sb.append(NEWLINE);
    }

    public void extraLine(int count) {
        while (count-- > 0) {
            printLineNumber(0);
            sb.append(NEWLINE);
        }
    }

    public void startMarker(int type) {
    }

    public void endMarker(int type) {
    }

    protected void printLineNumber(int lineNumber) {
        sb.append("/*");
        sb.append(String.format(format, ++realLineNumber));
        sb.append(':');
        sb.append(String.format(format, lineNumber));
        sb.append(" */ ");
    }
}
