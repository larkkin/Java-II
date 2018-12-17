package ru.spbau.mit.lara.client;


import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import ru.spbau.mit.lara.AbstractTorrentServer;
import ru.spbau.mit.lara.tracker.FileDescription;
import ru.spbau.mit.lara.tracker.StateWriter;
import ru.spbau.mit.lara.tracker.Tracker;
import ru.spbau.mit.lara.client.exceptions.ClientException;

public class Client extends AbstractTorrentServer {

    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    private static final int PERIOD = 1;
    private static final int STAT = 1;
    private static final int GET = 2;

    private final Map<Integer, P2PFile> files;
    private final FileWriter fileWriter;
    private final InetSocketAddress trackerAddress;
    private final String homeDir;

    public Client(InetSocketAddress trackerAddress, int port)
            throws IOException {
        this(trackerAddress, port, System.getProperty("user.dir"));
    }

    public Client(InetSocketAddress trackerAddress, int port, String homeDir)
            throws IOException {
        this.trackerAddress = trackerAddress;
        this.homeDir = homeDir;
        files = StateWriter.readClientFilesMap(homeDir);
        fileWriter = new FileWriter();
        start(port);
        update();
        executor.scheduleAtFixedRate(() -> {
            try {
                update();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, 0, PERIOD, TimeUnit.MINUTES);
    }

    @Override
    public void end() throws IOException {
        super.end();
        StateWriter.writeClientFilesMap(homeDir, files);
    }

    @Override
    protected void handleConnection(Socket socket) {
        try {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            while (socket.isConnected() && !socket.isClosed()) {
                synchronized (this) {
                    byte type = input.readByte();
                    switch (type) {
                        case STAT: {
                            int id = input.readInt();
                            P2PFile file = files.get(id);
                            Set<Integer> parts = file.getParts();
                            output.writeInt(parts.size());
                            for (int part : parts) {
                                output.writeInt(part);
                            }
                            break;
                        }
                        case GET: {
                            int id = input.readInt();
                            int part = input.readInt();
                            P2PFile file = files.get(id);
                            fileWriter.readPart(file, part, output);
                        }
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

    public void update() throws IOException {
        connectToTracker(((input, output) -> {
            output.writeByte(Tracker.Type.UPDATE);
            output.writeShort(localPort);
            output.writeInt(files.size());
            for (Integer id : files.keySet()) {
                output.writeInt(id);
            }
            boolean updated = input.readBoolean();
            if (!updated) {
                LOGGER.warning("Failed to update");
            }
        }));
    }

    public Collection<P2PFile> getFiles() {
        return files.values();
    }

    public boolean containsFile(FileDescription info) {
        P2PFile file = files.get(info.getId());
        return file != null && file.isFull();
    }

    public void addFile(String name) throws IOException {
        addFile(new File(homeDir, name));
    }

    public void addFile(File file) throws IOException {
        String name = file.getName();
        if (!file.exists()) {
            LOGGER.warning("File " + name + " does not exist.");
            return;
        }
        long size = file.length();
        connectToTracker((input, output) -> {
            output.writeByte(Tracker.Type.UPLOAD);
            output.writeUTF(name);
            output.writeLong(size);

            int id = input.readInt();
            files.put(id, P2PFile.createFull(file, size, id));
        });
        update();
    }

    public List<FileDescription> listFiles() throws IOException {
        List<FileDescription> result = new ArrayList<>();
        connectToTracker((input, output) -> {
            output.writeByte(Tracker.Type.LIST);
            int numFiles = input.readInt();
            for (int i = 0; i < numFiles; i++) {
                result.add(new FileDescription(input.readInt(), input.readUTF(), input.readLong()));
            }
        });
        return result;
    }

    public void getFile(FileDescription info, File fileTo)
            throws IOException, ExecutionException, InterruptedException, ClientException {
        P2PFile file = files.get(info.getId());
        if (file == null) {
            file = P2PFile.createEmpty(info, fileTo);
            files.put(info.getId(), file);
        }
        if (file.isFull()) {
            return;
        }
        loadFile(file);
    }

    public void getFile(int id) throws IOException, ExecutionException, InterruptedException, ClientException {
        for (FileDescription info : listFiles()) {
            if (id == info.getId()) {
                getFile(info, new File(homeDir, info.getName()));
                return;
            }
        }
        throw new FileNotFoundException("File with id = " + id + " not found.");
    }

    private void loadFile(P2PFile file)
            throws IOException, ExecutionException, InterruptedException, ClientException {
        List<InetSocketAddress> seeds = getSeeds(file.getId());
        if (seeds.size() == 0) {
            throw new ClientException(file.getName());
        }

        List<Future<?>> futures = new ArrayList<>();
        for (InetSocketAddress seed : seeds) {
            try {
                futures.add(executor.submit(new DownloadPartsTask(seed, file)));
            } catch (Exception e) {
                LOGGER.warning("Seed " + seed + " is unavailable");
            }
        }
        for (Future<?> future : futures) {
            future.get();
        }
        if (file.isFull()) {
            LOGGER.info("Successfully loaded file " + file.getFile());
        } else {
            throw new ClientException("Not fully loaded " + file.getName());
        }
    }

    private List<InetSocketAddress> getSeeds(int id) throws IOException {
        List<InetSocketAddress> seeds = new ArrayList<>();
        connectToTracker((input, output) -> {
            output.writeByte(Tracker.Type.SOURCES);
            output.writeInt(id);
            int numSeeds = input.readInt();
            for (int i = 0; i < numSeeds; i++) {
                byte[] address = new byte[4];
                for (int j = 0; j < 4; j++) {
                    address[j] = input.readByte();
                }
                int port = input.readShort() & 0xffff;
                InetAddress inetAddress = InetAddress.getByAddress(address);
                InetSocketAddress seedAddress = new InetSocketAddress(inetAddress, port);
                boolean isLoopback = inetAddress.equals(InetAddress.getLoopbackAddress());
                if (!isLoopback || port != localPort) {
                    seeds.add(seedAddress);
                }
            }
        });
        return seeds;
    }

    private void connectToTracker(ConnectionTask task) throws IOException {
        connect(trackerAddress, task);
    }

    private static void connect(InetSocketAddress address, ConnectionTask task) throws IOException {
        try (Socket socket = new Socket(address.getAddress(), address.getPort())) {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            task.process(input, output);
        }
    }

    private class DownloadPartsTask implements Runnable {
        private final InetSocketAddress seed;
        private final P2PFile file;

        public DownloadPartsTask(InetSocketAddress seed, P2PFile file) {
            this.seed = seed;
            this.file = file;
        }

        @Override
        public void run() {
            try {
                connect(seed, (input, output) -> {
                    int[] parts = collectParts(input, output);
                    for (int part : parts) {
                        if (!file.containsPart(part) && !file.isPartLoading(part)) {
                            file.startLoading(part);
                            loadPart(input, output, part);
                            System.out.println("part loaded: " + part + ", total = " + file.totalParts());

                        }
                    }
                    update();
                });
            } catch (IOException e) {
                LOGGER.warning("No connection to seed");
            }
        }

        private int[] collectParts(DataInputStream input, DataOutputStream output) throws IOException {
            output.writeByte(STAT);
            output.writeInt(file.getId());
            int numParts = input.readInt();
            int[] parts = new int[numParts];
            for (int i = 0; i < numParts; i++) {
                parts[i] = input.readInt();
            }
            return parts;
        }

        private void loadPart(DataInputStream input, DataOutputStream output, int part) throws IOException {
            output.writeByte(GET);
            output.writeInt(file.getId());
            output.writeInt(part);
            fileWriter.writePart(file, part, input);
            file.addPart(part);
        }

    }
    private interface ConnectionTask {
        void process(DataInputStream input, DataOutputStream output) throws IOException;
    }
}

