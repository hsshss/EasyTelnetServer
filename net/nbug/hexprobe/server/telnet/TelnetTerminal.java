package net.nbug.hexprobe.server.telnet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * TelnetTerminal
 * 
 * @author hexprobe <hexprobe@nbug.net>
 * 
 * @license
 * This code is hereby placed in the public domain.
 * 
 */
class TelnetTerminal {
	public static final int NUL = 0x00;
	public static final int BS = 0x08;
	public static final int HT = 0x09;
	public static final int LF = 0x0a;
	public static final int CR = 0x0d;
	public static final int ESC = 0x1b;
	public static final int DEL = 0x7f;
	
	public static final int UTF8_FIRST_BEGIN = 0x20;
	public static final int UTF8_FIRST_MAX_1 = 0x7f;
	public static final int UTF8_FIRST_MAX_2 = 0xdf;
	public static final int UTF8_FIRST_MAX_3 = 0xef;
	public static final int UTF8_FIRST_MAX_4 = 0xf7;
	public static final int UTF8_FIRST_MAX_5 = 0xfb;
	public static final int UTF8_FIRST_MAX_6 = 0xfd;
	public static final int UTF8_FIRST_END = UTF8_FIRST_MAX_6;
	
	public static final int IAC = 0xff;
	public static final int IAC_WILL = 0xfb;
	public static final int IAC_DO = 0xfd;
	public static final int IAC_ECHO = 0x01;
	public static final int IAC_BINARY = 0x00;
	public static final int IAC_SGA = 0x03;
	public static final int IAC_NAWS = 0x1f;
	public static final int IAC_SB = 0xfa;
	public static final int IAC_SE = 0xf0;
	
	public static final int CSI = 0x5b;
	public static final int CSI_FINAL_BEGIN = 0x40;
	public static final int CSI_FINAL_END = 0x7e;

	private final Charset encoding;
	private final DataOutputStream out;
	private final DataInputStream in;
	
	private String prompt = "> ";
	private OnCommandLineListener onCommandLineListener = null;
	
	public TelnetTerminal(OutputStream out, InputStream in) {
		this.encoding = Charset.forName("UTF-8");
		this.out = new DataOutputStream(new BufferedOutputStream(out));
		this.in = new DataInputStream(new BufferedInputStream(in));
	}
	
	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}
	
	public void setOnCommandLineListener(OnCommandLineListener onCommandLineListener) {
		this.onCommandLineListener = onCommandLineListener;
	}

	public void run() throws IOException {
		final VT100Terminal terminal = new VT100Terminal(out, in, encoding);
		final StringBuilder lineBuf = new StringBuilder();
		
		terminal.setOnClearScreenListener(new OnClearScreenListener() {
			@Override
			public void onClearScreen() throws IOException {
				terminal.write(prompt);
				terminal.write(lineBuf.toString());
				terminal.flush();
			}
		});
		
		writeBytes(out, IAC, IAC_WILL, IAC_ECHO);
		writeBytes(out, IAC, IAC_DO, IAC_SGA);
		writeBytes(out, IAC, IAC_WILL, IAC_SGA);
		writeBytes(out, IAC, IAC_DO, IAC_BINARY);
		writeBytes(out, IAC, IAC_WILL, IAC_BINARY);
		writeBytes(out, IAC, IAC_DO, IAC_NAWS);
		
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		
		terminal.write(prompt);
		terminal.flush();
		
		while (true) {
			int b = read(in);
			
			if (UTF8_FIRST_BEGIN <= b && b <= UTF8_FIRST_END && b != DEL) {
				buf.reset();
				buf.write(b);
				
				int n = 0;
				
				if (b <= UTF8_FIRST_MAX_1) {
					n = 0;
				} else if (b <= UTF8_FIRST_MAX_2) {
					n = 1;
				} else if (b <= UTF8_FIRST_MAX_3) {
					n = 2;
				} else if (b <= UTF8_FIRST_MAX_4) {
					n = 3;
				} else if (b <= UTF8_FIRST_MAX_5) {
					n = 4;
				} else if (b <= UTF8_FIRST_MAX_6) {
					n = 5;
				}
				
				for (; n > 0; n--) {
					buf.write(read(in));
				}
				
				String tmp = buf.toString(encoding.name());
				lineBuf.append(tmp);
				terminal.write(tmp);
				terminal.flush();
			} else {
				switch (b) {
				case CR:
					terminal.write("\r\n");
					terminal.flush();
					
					
					if (onCommandLineListener != null) {
						String line = lineBuf.toString();
						lineBuf.setLength(0);
						onCommandLineListener.OnCommandLine(terminal, line);
					} else {
						lineBuf.setLength(0);
					}
					
					terminal.write(prompt);
					terminal.flush();
					break;
					
				case DEL:
				case BS:
					if (lineBuf.length() > 0) {
						terminal.backSpace();
						String tmp = biteTail(lineBuf.toString());
						lineBuf.setLength(0);
						lineBuf.append(tmp);
					}
					break;
					
				case ESC:
					b = read(in);
					switch (b) {
					case CSI:
						do {
							b = read(in);
						} while (!(CSI_FINAL_BEGIN <= b && b <= CSI_FINAL_END));
						break;
					}
					break;
					
				case IAC:
					b = read(in);
					switch (b) {
					case IAC_SB:
						b = read(in);
						switch (b) {
						case IAC_NAWS:
							short width = in.readShort();
							short height = in.readShort();
							terminal.setScreenSize(width, height);
							break;
						}
						break;
						
					case IAC_SE:
						break;
						
					default:
						read(in);
					}
					break;
				}
			}
		}
	}
	
	private static int read(InputStream s) throws IOException {
		int b = s.read();
		if (b >= 0) {
			return b;
		} else {
			throw new IOException();
		}
	}
	
	private static void writeBytes(OutputStream s, int... b) throws IOException {
		for (int i : b) {
			s.write(i);
		}
	}
	
	private static String biteTail(String s) {
		char[] str = s.toCharArray();
		for (int i = str.length - 1; i >= 0; i--) {
			if (!Character.isLowSurrogate(str[i])) {
				return new String(str, 0, i);
			}
		}
		return "";
	}
}
