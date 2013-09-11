package net.nbug.hexprobe.server.telnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

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
class VT100Terminal implements EasyTerminal {
    public static final int ESC = 0x1b;
    public static final int DEL = 0x7f;

    private static final int TAB_SIZE = 8;

    private static final byte NONE = 0;
    private static final byte FIRST = 1;
    private static final byte SUBSEQ = 2;

    private final Charset encoding;
    private final OutputStream out;
    private final InputStream in;

    private OnClearScreenListener onClearScreenListener = null;

    private int x = 0;
    private int y = 0;
    private int width = 80;
    private int height = 24;
    private byte[] screen = null;
    private boolean logMode = false;

    public VT100Terminal(OutputStream out, InputStream in, Charset encoding) {
        this.encoding = encoding;
        this.out = out;
        this.in = in;
        this.screen = new byte[height * width];
    }

    public void setOnClearScreenListener(OnClearScreenListener onClearScreenListener) {
        this.onClearScreenListener = onClearScreenListener;
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
    public boolean isLogMode() {
        return logMode;
    }

    @Override
    public void setLogMode(boolean logMode) {
        this.logMode = logMode;
    }

    @Override
    public void write(String s) throws IOException {
        char hi = 0;
        char lo = 0;
        int w = 0;

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

                w = StringUtils.getPhisicalWidth(hi, lo);

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
    public void flush() throws IOException {
        out.flush();
    }
    
    public void setScreenSize(int width, int height) throws IOException {
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;

            screen = new byte[height * width];

            clearScreen();
        }
    }

    public boolean backSpace() throws IOException {
        int i = 0;
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

            return true;
        } else {
            return false;
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

    public void clearScreen() throws IOException {
        x = 0;
        y = 0;

        out.write(ESC);
        out.write("[1;1H".getBytes(encoding.name()));

        Arrays.fill(screen, NONE);

        out.write(ESC);
        out.write("[J".getBytes(encoding.name()));
        out.flush();

        if (onClearScreenListener != null) {
            onClearScreenListener.onClearScreen();
        }
    }
}
