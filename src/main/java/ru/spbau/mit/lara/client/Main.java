package ru.spbau.mit.lara.client;

import ru.spbau.mit.lara.tracker.FileDescription;
import ru.spbau.mit.lara.tracker.Tracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

public class Main {
    private static Client client = null;

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: <tracker_ip> <port>");
            return;
        }
        InetSocketAddress trackerAddress = new InetSocketAddress(InetAddress.getByName(args[0]), Tracker.PORT);
        int port = Integer.parseInt(args[1]);
        client = new Client(trackerAddress, port);

        try (BufferedReader br =  new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                String inputLine = br.readLine();
                String[] arr = inputLine.split(" ");
                String command = arr[0];
                try {
                    switch (command) {
                        case "list": {
                            list();
                            break;
                        }
                        case "add": {
                            if (arr.length < 2) {
                                printUsage();
                                break;
                            }
                            client.addFile(arr[1]);
                            break;
                        }
                        case "get": {
                            if (arr.length < 2) {
                                printUsage();
                                break;
                            }
                            client.getFile(Integer.parseInt(arr[1]));
                            break;
                        }
                        case "end": {
                            client.end();
                            return;
                        }
                        default:
                            printUsage();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to execute command");
                    e.printStackTrace();
                }
            }
        }
    }

    private static void list() throws IOException {
        List<FileDescription> files = client.listFiles();
        System.out.println("Number of files = " + files.size());
        for (int i = 0; i < files.size(); i++) {
            FileDescription file = files.get(i);
            System.out.println((i + 1) + ": " + "id = " + file.getId() +
                                ", name = " + file.getName() + ", size = " + file.getSize());
        }
    }

    private static void printUsage() {
        System.out.println("Usage:\n\tlist | add <file_name> | get <file_id> | exit");
    }
}
