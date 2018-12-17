package ru.spbau.mit.lara.tracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ru.spbau.mit.lara.client.P2PFile;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StateWriter {
    public static final String TRACKER_FILES_PATH = "tracker_files.json";
    public static final String CLIENT_FILES_PATH = "client_files.json";

    public static List<FileDescription> readTrackerFilesList(String home) {
        File stored = new File(home, TRACKER_FILES_PATH);
        if (!stored.exists()) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        try (Reader reader = new FileReader(stored)) {
            Type type = new TypeToken<List<FileDescription>>() { }.getType();
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            System.err.println("Failed to load saved files");
            return new ArrayList<>();
        }
    }

    public static void writeTrackerFilesList(String rootDir, List<FileDescription> files) throws IOException {
        File outputFile = new File(rootDir, TRACKER_FILES_PATH);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (PrintWriter writer = new PrintWriter(outputFile)) {
            String jsonStr = gson.toJson(files);
            writer.println(jsonStr);
        }
    }

    public static Map<Integer, P2PFile> readClientFilesMap(String home) {
        File stored = new File(home, CLIENT_FILES_PATH);
        if (!stored.exists()) {
            return new HashMap<>();
        }
        Gson gson = new Gson();
        try (Reader reader = new FileReader(stored)) {
            Type type = new TypeToken<Map<Integer, P2PFile>>() { }.getType();
            //noinspection unchecked
            return (Map<Integer, P2PFile>) gson.fromJson(reader, type);
        } catch (Exception e) {
            System.err.println("Failed to load saved files");
            return new HashMap<>();
        }
    }
    public static void writeClientFilesMap(String home, Map<Integer, P2PFile> files) throws IOException {
        File stored = new File(home, CLIENT_FILES_PATH);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (PrintWriter writer = new PrintWriter(stored)) {
            String json = gson.toJson(files);
            writer.println(json);
        }
    }

}
