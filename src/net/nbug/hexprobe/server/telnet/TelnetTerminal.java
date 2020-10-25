package net.nbug.hexprobe.server.telnet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
    public static final int IAC = 0xff;
    public static final int IAC_WILL = 0xfb;
    public static final int IAC_DO = 0xfd;
    public static final int IAC_ECHO = 0x01;
    public static final int IAC_BINARY = 0x00;
    public static final int IAC_SGA = 0x03;
    public static final int IAC_NAWS = 0x1f;

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

        terminal.setOnClearScreenListener(new OnClearScreenListener() {
            @Override
            public void onClearScreen() throws IOException {
                terminal.write(prompt);
                terminal.flush();
            }
        });

        writeBytes(out, IAC, IAC_WILL, IAC_ECHO);
        writeBytes(out, IAC, IAC_DO, IAC_SGA);
        writeBytes(out, IAC, IAC_WILL, IAC_SGA);
        writeBytes(out, IAC, IAC_DO, IAC_BINARY);
        writeBytes(out, IAC, IAC_WILL, IAC_BINARY);
        writeBytes(out, IAC, IAC_DO, IAC_NAWS);

        while (true) {
            terminal.write(prompt);
            terminal.flush();

            String line = terminal.readLine();
            if (onCommandLineListener != null) {
                onCommandLineListener.OnCommandLine(terminal, line);
            }
        }
    }

    private static void writeBytes(OutputStream s, int... b) throws IOException {
        for (int i : b) {
            s.write(i);
        }
    }
}
