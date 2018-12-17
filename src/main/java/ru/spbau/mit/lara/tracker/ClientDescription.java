package ru.spbau.mit.lara.tracker;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ClientDescription {

    private static final long INTERVAL = TimeUnit.MINUTES.toMillis(5);

    private final byte[] address;
    private final short port;
    private final Set<Integer> files = new HashSet<>();
    private long heartbeatTime;

    public ClientDescription(byte[] address, short port) {
        this.address = address;
        this.port = port;
        heartbeatTime = System.currentTimeMillis();
    }

    public void writeTo(DataOutputStream output) throws IOException {
        for (int i = 0; i < 4; i++) {
            output.writeByte(address[i]);
        }
        output.writeShort(port);
    }

    public void addFile(int id) {
        files.add(id);
    }

    public boolean hasFile(int id) {
        return files.contains(id);
    }

    public boolean isActive() {
        long currentTime = System.currentTimeMillis();
        return currentTime - heartbeatTime < INTERVAL;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClientDescription)) {
            return false;
        }
        ClientDescription other = (ClientDescription) obj;
        return Arrays.equals(address, other.address) && port == other.port;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(address) * 31 + port;
    }
}