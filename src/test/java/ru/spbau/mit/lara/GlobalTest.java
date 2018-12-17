package ru.spbau.mit.lara;

import org.apache.commons.io.FileUtils;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import ru.spbau.mit.lara.client.Client;
import ru.spbau.mit.lara.client.P2PFile;
import ru.spbau.mit.lara.tracker.FileDescription;
import ru.spbau.mit.lara.tracker.Tracker;


import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GlobalTest {

    private Tracker tracker;
    private File folder1;
    private Client client1;
    private File folder2;
    private Client client2;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    private final Random random = new Random();

    @Before
    public void setup() throws Exception {
        File trackerFolder = tempFolder.newFolder();
        tracker = new Tracker(trackerFolder.getPath());

        InetSocketAddress trackerAddress = new InetSocketAddress(InetAddress.getLocalHost(), Tracker.PORT);
        folder1 = tempFolder.newFolder();
        client1 = new Client(trackerAddress, 8881, folder1.getPath());
        folder2 = tempFolder.newFolder();
        client2 = new Client(trackerAddress, 8882, folder2.getPath());
    }

    @After
    public void teardown() throws IOException {
        tracker.end();
        client1.end();
        client2.end();
    }

    @Test
    public void addFile() throws IOException {
        File file = new File(folder1, "1.txt");
        FileUtils.writeStringToFile(file, "hey pam pam pam", Charset.defaultCharset());

        List<FileDescription> files = client1.listFiles();
        System.out.println(files.size());
        Assert.assertTrue(files.isEmpty());

        client1.addFile(file);
        files = client1.listFiles();
        Assert.assertEquals(1, files.size());

        FileDescription info = files.get(0);
        Assert.assertEquals(file.getName(), info.getName());
        Assert.assertEquals(file.length(), info.getSize());
        Assert.assertEquals(1, info.getId());
    }


    @Test
    public void loadFile() throws Exception {
        String name = new BigInteger(100, random).toString(32);
        File file = new File(folder1, name);

        String content = new BigInteger(100000, random).toString(32);
        FileUtils.writeStringToFile(file, content, Charset.defaultCharset());

        client1.addFile(name);
        List<FileDescription> files = client2.listFiles();
        FileDescription last = files.get(files.size() - 1);
        client2.getFile(last.getId());

        File loaded = new File(folder2, name);
        String result = FileUtils.readFileToString(loaded, Charset.defaultCharset());
        Assert.assertEquals(result, content);
    }


    @Test
    public void tenClients() throws Exception {
        final int NUM_CLIENTS = 10;

        List<File> folders = new ArrayList<>();
        List<Client> clients = new ArrayList<>();
        InetSocketAddress trackerAddress = new InetSocketAddress(InetAddress.getLocalHost(), Tracker.PORT);

        for (int i = 0; i < NUM_CLIENTS; i++) {
            File folder = tempFolder.newFolder();
            folders.add(folder);
            clients.add(new Client(trackerAddress, 0, folder.getPath()));
        }

        String name = new BigInteger(100, random).toString(32);
        File file = new File(folders.get(0), name);
        String content = new BigInteger(P2PFile.PART_SIZE + 1, random).toString(32);
        FileUtils.writeStringToFile(file, content, Charset.defaultCharset());
        clients.get(0).addFile(name);
        int id = 1;

        for (int i = 1; i < NUM_CLIENTS; i++) {
            clients.get(i).getFile(id);
            File loaded = new File(folders.get(i), name);
            String result = FileUtils.readFileToString(loaded, Charset.defaultCharset());
            Assert.assertEquals(result, content);
        }
        for (Client client : clients) {
            client.end();
        }
    }


}
