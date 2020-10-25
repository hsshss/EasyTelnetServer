package net.nbug.hexprobe.server.telnet;

import java.io.IOException;

/**
 * OnCommandLineListener
 *
 * @author hexprobe <hexprobe@nbug.net>
 *
 * @license
 * This code is hereby placed in the public domain.
 *
 */
public interface OnCommandLineListener {
    void OnCommandLine(EasyTerminal terminal, String commandLine) throws IOException;
}
