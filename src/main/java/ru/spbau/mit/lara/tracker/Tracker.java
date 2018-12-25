package ru.spbau.mit.lara.tracker;

import ru.spbau.mit.lara.AbstractTorrentServer;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Tracker extends AbstractTorrentServer {
    public static final int PORT = 8081;
    private static final Logger LOGGER = Logger.getLogger(Tracker.class.getName());
    private int maxUsedId;
    private final String rootDir;
    private final List<FileDescription> filesList;
    private final Set<ClientDescription> clientsList = new HashSet<>();


    public static final class Type {
        public static final byte LIST = 1;
        public static final byte UPLOAD = 2;
        public static final byte SOURCES = 3;
        public static final byte UPDATE = 4;
    }


    public Tracker(String rootDir) throws IOException {
        this.rootDir = rootDir;
        filesList = StateWriter.readTrackerFilesList(rootDir);
        maxUsedId = filesList.isEmpty() ? 0 : filesList.get(filesList.size() - 1).getId();
        start(PORT);
    }

    public Tracker() throws IOException {
        this(System.getProperty("user.dir"));
    }


    @Override
    public void end() throws IOException {
        super.end();
        StateWriter.writeTrackerFilesList(rootDir, filesList);
    }


    @Override
    public void handleConnection(Socket socket) {
        try {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            while (socket.isConnected() && !socket.isClosed()) {
                synchronized (this) {
                    InetAddress address = socket.getInetAddress();
                    byte type = input.readByte();
                    switch (type) {
                        case Type.LIST:
                            listFiles(output);
                            break;
                        case Type.UPLOAD:
                            int id = addFile(input.readUTF(), input.readLong());
                            output.writeInt(id);
                            break;
                        case Type.SOURCES:
                            listSources(input.readInt(), output);
                            break;
                        case Type.UPDATE:
                            processUpdate(address, input, output);
                    }
                }
                output.flush();
            }
        } catch (SocketException | EOFException ignored) {
            // disconnected
        } catch (IOException e) {
            LOGGER.warning("Error in connection: ");
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void listFiles(DataOutputStream output) throws IOException {
        output.writeInt(filesList.size());
        for (FileDescription file : filesList) {
            file.writeTo(output);
        }
    }

    private int addFile(String name, long size) {
        int id = maxUsedId + 1;
        maxUsedId++;
        filesList.add(new FileDescription(id, name, size));
        LOGGER.info("Added file: name = " + name + ", id = " + id);
        return id;
    }

    private void listSources(int id, DataOutputStream output) throws IOException {
        LOGGER.info("List sources for file: id = " + id);
        List<ClientDescription> sources = clientsList.stream()
                .filter(client -> client.hasFile(id) && client.isActive())
                .collect(Collectors.toList());
        output.writeInt(sources.size());
        for (ClientDescription client : sources) {
            client.writeTo(output);
        }
    }

    private void processUpdate(InetAddress address, DataInputStream input, DataOutputStream output) throws IOException {
        short port = input.readShort();
        LOGGER.info("Updating client: ip = " + Arrays.toString(address.getAddress()) + ", port = " + port);
        ClientDescription client = new ClientDescription(address.getAddress(), port);
        clientsList.remove(client);
        int numFiles = input.readInt();
        for (int i = 0; i < numFiles; i++) {
            int id = input.readInt();
            client.addFile(id);
        }
        clientsList.add(client);
        output.writeBoolean(true);
    }
}

