package net.nbug.hexprobe.server.telnet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * EasyTerminal
 *
 * @author hexprobe <hexprobe@nbug.net>
 *
 * @license
 * This code is hereby placed in the public domain.
 *
 */
public interface EasyTerminal {
    String readLine() throws IOException;
    void write(String s) throws IOException;
    void writeLine(String s) throws IOException;
    void flush() throws IOException;
    void close() throws IOException;
    InputStream getInputStream();
    Charset getEncoding();
    boolean isEcho();
    void setEcho(boolean enable);
    boolean isLogMode();
    void setLogMode(boolean logMode);
}
