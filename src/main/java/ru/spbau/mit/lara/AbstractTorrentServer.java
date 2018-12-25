package ru.spbau.mit.lara;



import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

public abstract class AbstractTorrentServer implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(AbstractTorrentServer.class.getName());

    protected final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    protected ServerSocket serverSocket;
    protected int localPort;

    public void start(int port) throws IOException {
        LOGGER.info("Started server at port " + port);
        localPort = port;
        executor.submit(this);
    }

    public void end() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
        executor.shutdown();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(localPort);
            if (localPort == 0) {
                localPort = serverSocket.getLocalPort();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                executor.submit(() -> handleConnection(socket));
            } catch (IOException e) {
                if (serverSocket.isClosed()) {
                    LOGGER.info("Socket is closed");
                    return;
                } else {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    protected abstract void handleConnection(Socket socket);
}
