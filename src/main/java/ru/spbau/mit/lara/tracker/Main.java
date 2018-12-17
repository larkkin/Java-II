package ru.spbau.mit.lara.tracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {
        Tracker tracker = new Tracker();
        try (BufferedReader br =  new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                String inputLine = br.readLine();
                if (inputLine.equals("end")) {
                    tracker.end();
                    return;
                }
            }
        }
    }
}
