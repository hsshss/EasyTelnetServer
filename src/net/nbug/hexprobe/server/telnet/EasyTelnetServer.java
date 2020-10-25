package net.nbug.hexprobe.server.telnet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * EasyTelnetServer
 *
 * @author hexprobe <hexprobe@nbug.net>
 *
 * @license
 * This code is hereby placed in the public domain.
 *
 */
public class EasyTelnetServer {
    private String prompt = null;
    private ServerWorker srv = null;
    private OnCommandLineListener onCommandLineListener = null;

    public void start(int port) throws IOException {
        if (srv == null) {
            ServerSocket sock = new ServerSocket(port);
            srv = new ServerWorker(sock);
            srv.start();
        } else {
            throw new IllegalStateException();
        }
    }

    public void stop() throws InterruptedException {
        if (srv != null) {
            srv.terminate();
            srv.join();
            srv = null;
        } else {
            throw new IllegalStateException();
        }
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void setOnCommandLineListener(OnCommandLineListener onCommandLineListener) {
        this.onCommandLineListener = onCommandLineListener;
    }

    private class ServerWorker extends Thread {
        private final ServerSocket ssock;
        private volatile boolean terminated = false;

        public ServerWorker(ServerSocket ssock) {
            this.ssock = ssock;
        }

        public void terminate() {
            terminated = true;

            if (!ssock.isClosed()) {
                try {
                    ssock.close();
                } catch (IOException e) {
                    // Do nothing
                }
            }
        }

        @Override
        public void run() {
            try {
                while (!terminated) {
                    Socket sock = ssock.accept();
                    ClientWorker cli = new ClientWorker(sock);
                    cli.start();
                }
            } catch (IOException e) {
                // Do nothing
            } finally {
                if (!ssock.isClosed()) {
                    try {
                        ssock.close();
                    } catch (IOException e) {
                        // Do nothing
                    }
                }
            }
        }
    }

    private class ClientWorker extends Thread {
        private final Socket sock;

        public ClientWorker(Socket sock) {
            this.sock = sock;
        }

        @Override
        public void run() {
            try {
                TelnetTerminal telnet =
                    new TelnetTerminal(
                        new DataOutputStream(sock.getOutputStream()),
                        new DataInputStream(sock.getInputStream()),
                        Charset.forName("UTF-8"));
                if (prompt != null) {
                    telnet.setPrompt(prompt);
                }
                telnet.setOnCommandLineListener(onCommandLineListener);
                telnet.run();
            } catch (IOException e) {
                // Do nothing
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (!sock.isClosed()) {
                    try {
                        sock.close();
                    } catch (IOException e) {
                        // Do nothing
                    }
                }
            }
        }
    }
}
