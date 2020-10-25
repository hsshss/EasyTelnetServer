package net.nbug.hexprobe.server.telnet;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.nbug.hexprobe.util.StringUtils;

/**
 * VT100Terminal
 *
 * @author hexprobe <hexprobe@nbug.net>
 *
 * @license
 * This code is hereby placed in the public domain.
 *
 */
class TelnetTerminal implements EasyTerminal {
    public static final int BS = 0x08;
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

    private static final int TAB_SIZE = 8;

    private static final byte NONE = 0;
    private static final byte FIRST = 1;
    private static final byte SUBSEQ = 2;

    private final Charset encoding;
    private final DataOutputStream out;
    private final DataInputStream in;
    private final Map<String, Object> session;

    private String prompt = "> ";
    private OnCommandLineListener onCommandLineListener = null;

    private int x = 0;
    private int y = 0;
    private int width = 80;
    private int height = 24;
    private byte[] screen;
    private boolean echo = true;
    private boolean logMode = false;

    public TelnetTerminal(DataOutputStream out, DataInputStream in, Charset encoding) {
        this.encoding = encoding;
        this.out = out;
        this.in = in;
        this.screen = new byte[height * width];
        this.session = new HashMap<String, Object>();
    }

    public void run() throws IOException {
        writeInitialSequence();

        while (true) {
            writePrompt();

            String line = readLine();
            if (onCommandLineListener != null) {
                onCommandLineListener.OnCommandLine(this, line);
            }
        }
    }

    public void setOnCommandLineListener(OnCommandLineListener onCommandLineListener) {
        this.onCommandLineListener = onCommandLineListener;
    }

    @Override
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public InputStream getInputStream() {
        return in;
    }

    @Override
    public Charset getEncoding() {
        return encoding;
    }

    @Override
    public boolean isEcho() {
        return echo;
    }

    @Override
    public void setEcho(boolean enable) {
        echo = enable;
    }

    @Override
    public boolean isLogMode() {
        return logMode;
    }

    @Override
    public void setLogMode(boolean logMode) {
        this.logMode = logMode;
    }

    @Override
    public Set<String> getSessionKeys() {
        return session.keySet();
    }

    @Override
    public Object getSession(String key) {
        return session.get(key);
    }

    @Override
    public void setSession(String key, Object value) {
        session.put(key, value);
    }

    @Override
    public String readLine() throws IOException {
        StringBuilder lineBuf = new StringBuilder();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        while (true) {
            int b = read(in);

            if (UTF8_FIRST_BEGIN <= b && b <= UTF8_FIRST_END && b != DEL) {
                buf.reset();
                buf.write(b);

                int n;

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
                } else {
                    n = 0;
                }

                for (; n > 0; n--) {
                    buf.write(read(in));
                }

                String tmp = buf.toString(encoding.name());
                lineBuf.append(tmp);
                if (echo) {
                    write(tmp);
                    flush();
                }
            } else {
                switch (b) {
                case CR:
                    if (echo) {
                        writeLine("");
                        flush();
                    }
                    return lineBuf.toString();

                case DEL:
                case BS:
                    if (lineBuf.length() > 0) {
                        backSpace();
                        String tmp = biteTail(lineBuf.toString());
                        lineBuf.setLength(0);
                        lineBuf.append(tmp);
                    }
                    break;

                case ESC:
                    b = read(in);
                    if (b == CSI) {
                        do {
                            b = read(in);
                        } while (!(CSI_FINAL_BEGIN <= b && b <= CSI_FINAL_END));
                    }
                    break;

                case IAC:
                    b = read(in);
                    switch (b) {
                    case IAC_SB:
                        b = read(in);
                        if (b == IAC_NAWS) {
                            short width = in.readShort();
                            short height = in.readShort();
                            setScreenSize(width, height);
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

    @Override
    public void write(String s) throws IOException {
        char hi = 0;
        char lo;
        int w;

        for (char c : s.toCharArray()) {
            if (Character.isHighSurrogate(c)) {
                hi = c;
                continue;
            } else if (Character.isLowSurrogate(c)) {
                lo = c;
            } else {
                hi = c;
                lo = 0;
            }

            switch (hi) {
            case '\r':
                x = 0;
                out.write(String.valueOf(hi).getBytes(encoding.name()));
                break;

            case '\n':
                out.write(String.valueOf(hi).getBytes(encoding.name()));
                newLine(false);
                break;

            case '\t':
                int newX = x + TAB_SIZE;
                newX -= newX % TAB_SIZE;

                for (int i = 0; x < newX && x < width; x++, i++) {
                    screen[y * width + x] = i == 0 ? FIRST : SUBSEQ;
                }

                out.write(String.valueOf(hi).getBytes(encoding.name()));

                if (x == width) {
                    if (logMode) {
                        x = 0;
                        newLine(true);
                    } else {
                        out.write(" \r".getBytes(encoding.name()));

                        x = 0;
                        newLine(false);
                    }
                }
                break;

            default:
                if (hi < ' ' || hi == DEL) {
                    break;
                }

                w = StringUtils.getPhysicalWidth(hi);

                if (x + w > width) {
                    if (logMode) {
                        x = 0;
                        newLine(true);
                    } else {
                        for (; x < width; x++) {
                            screen[y * width + x] = NONE;
                            out.write(" ".getBytes(encoding.name()));
                        }
                        out.write(" \r".getBytes(encoding.name()));

                        x = 0;
                        newLine(false);
                    }
                }

                for (int i = 0; i < w; i++, x++) {
                    screen[y * width + x] = i == 0 ? FIRST : SUBSEQ;
                }

                if (lo > 0) {
                    out.write(String.valueOf(new char[]{hi, lo}).getBytes(encoding.name()));
                } else {
                    out.write(String.valueOf(hi).getBytes(encoding.name()));
                }

                if (x == width) {
                    if (logMode) {
                        x = 0;
                        newLine(true);
                    } else {
                        out.write(" \r".getBytes(encoding.name()));

                        x = 0;
                        newLine(false);
                    }
                }
            }
        }
    }

    @Override
    public void writeLine(String s) throws IOException {
        write(s);
        write("\r\n");
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    private void writePrompt() throws IOException {
        write(prompt);
        flush();
    }

    private void writeInitialSequence() throws IOException {
        writeBytes(out, IAC, IAC_WILL, IAC_ECHO);
        writeBytes(out, IAC, IAC_DO, IAC_SGA);
        writeBytes(out, IAC, IAC_WILL, IAC_SGA);
        writeBytes(out, IAC, IAC_DO, IAC_BINARY);
        writeBytes(out, IAC, IAC_WILL, IAC_BINARY);
        writeBytes(out, IAC, IAC_DO, IAC_NAWS);
    }
    
    private void setScreenSize(int width, int height) throws IOException {
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;

            screen = new byte[height * width];

            clearScreen();
        }
    }

    private void backSpace() throws IOException {
        int i;
        boolean found = false;
        int orgX = x;
        int orgY = y;

        for (i = y * width + x; i >= 0; i--) {
            byte crr = screen[i];
            screen[i] = NONE;
            if (crr == FIRST) {
                found = true;
                break;
            }
        }

        if (found) {
            for (; i >= 0; i--) {
                if (screen[i] != NONE) {
                    break;
                }
            }
            i++;

            x = i % width;
            y = i / width;

            if (moveRelative(x - orgX, y - orgY)) {
                out.write(ESC);
                out.write("[J".getBytes(encoding.name()));
                out.flush();
            }
        }
    }
    
    private boolean moveRelative(int offX, int offY) throws IOException {
        if (offX > 0) {
            out.write(ESC);
            out.write(String.format("[%dC", offX).getBytes(encoding.name()));
        } else if(offX < 0) {
            out.write(ESC);
            out.write(String.format("[%dD", -offX).getBytes(encoding.name()));
        }
        
        if (offY > 0) {
            out.write(ESC);
            out.write(String.format("[%dB", offY).getBytes(encoding.name()));
        } else if(offY < 0) {
            out.write(ESC);
            out.write(String.format("[%dA", -offY).getBytes(encoding.name()));
        }
        
        return offX != 0 || offY != 0;
    }

    private void newLine(boolean move) throws IOException {
        y += 1;
        if (y >= height) {
            y = height - 1;

            for (int i = 0; i < height - 1; i++) {
                for (int j = 0; j < width; j++) {
                    screen[i * width + j] = screen[(i + 1) * width + j];
                }
            }

            Arrays.fill(screen, (height - 1) * width, screen.length, NONE);

            if (move) {
                out.write(ESC);
                out.write("[S".getBytes(encoding.name()));
            }
        }

        if (move) {
            out.write(ESC);
            out.write("[E".getBytes(encoding.name()));
        }
    }

    private void clearScreen() throws IOException {
        x = 0;
        y = 0;

        out.write(ESC);
        out.write("[1;1H".getBytes(encoding.name()));

        Arrays.fill(screen, NONE);

        out.write(ESC);
        out.write("[J".getBytes(encoding.name()));
        out.flush();

        writePrompt();
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
