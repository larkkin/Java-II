package ru.spbau.mit.lara.tracker;

import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

public class FileDescription {
    private final int id;
    private final String name;
    private final long size;

    public FileDescription(int id, String name, long size) {
        this.id = id;
        this.name = name;
        this.size = size;
    }

    public void writeTo(DataOutputStream output) throws IOException {
        output.writeInt(id);
        output.writeUTF(name);
        output.writeLong(size);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FileDescription && ((FileDescription) obj).id == id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return id + ": " + name + ", " + toReadableSize(size);
    }

    public static String toReadableSize(long size) {
        String[] units = new String[]{"B", "kB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
