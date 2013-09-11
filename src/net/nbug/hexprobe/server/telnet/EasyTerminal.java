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
	public void write(String s) throws IOException;
	public void flush() throws IOException;
	public void close() throws IOException;
	public InputStream getInputStream();
	public Charset getEncoding();
    public boolean isLogMode();
    public void setLogMode(boolean logMode);
}
