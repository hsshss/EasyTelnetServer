package net.nbug.hexprobe.server.telnet;

import java.io.IOException;

/**
 * OnClearScreenListener
 *
 * @author hexprobe <hexprobe@nbug.net>
 *
 * @license
 * This code is hereby placed in the public domain.
 *
 */
interface OnClearScreenListener {
    void onClearScreen() throws IOException;
}
