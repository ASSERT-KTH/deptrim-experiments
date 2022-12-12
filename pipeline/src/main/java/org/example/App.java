package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {

        // iterate over folders in a directory
        try {
            Files.list(Paths.get("results"))
                    .filter(Files::isDirectory)
                    .forEach(System.out::println);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
