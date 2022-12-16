package se.kth.castor.deptrim.experiments;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class App {


    public static void main(String[] args) throws IOException {

        File splDeps = new File("specialized-deps.csv");
        File origDeps = new File("original-deps.csv");
        File deptrimLogs = new File("deptrim-logs.csv");

        try (Stream<Path> filepath = Files.walk(Paths.get("results"))) {
            // process the /deptrim/ directory
            filepath.filter(Files::isRegularFile).forEach(f -> {
                if (f.toString().contains("deptrim/libs-specialized") && f.toString().endsWith(".jar")) {
                    processSpecializedJars(splDeps, f);
                } else if (f.toString().contains("original/compile-scope-dependencies")) {
                    processCompileScopeJars(origDeps, f);
                } else if (f.toString().endsWith("deptrim/deptrim.log")) {
                    processDeptrimLogs(deptrimLogs, f);
                }
            });
        } catch (IOException e) {
            throw new IOException("Directory Not Present!");
        }
    }

    private static void processSpecializedJars(File splDeps, Path f) {
        List<String> l = new ArrayList<>();
        l.add(f + "," + FileUtils.sizeOf(new File(f.toString())) + "\n");
        String str = String.join("", l);
        try {
            FileUtils.writeStringToFile(splDeps, str, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processCompileScopeJars(File origDeps, Path f) {
        List<String> l = new ArrayList<>();
        l.add(f + "," + FileUtils.sizeOf(new File(f.toString())) + "\n");
        String str = String.join("", l);
        try {
            FileUtils.writeStringToFile(origDeps, str, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processDeptrimLogs(File depLogs, Path f) {
        List<String> l = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(f);
            for (String line : lines) {
                if (line.contains("[INFO] Specializing dependency")) {
                    String specializedDependency = line.split(" ")[3];
                    String nbTypesRemoved = line.split(" ")[5].split("/")[0];
                    String nbTypesTotal = line.split(" ")[5].split("/")[1];
                    l.add(f + specializedDependency + "," + nbTypesRemoved + "," + nbTypesTotal + "\n");
                }
            }
            String str = String.join("", l);
            try {
                FileUtils.writeStringToFile(depLogs, str, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
