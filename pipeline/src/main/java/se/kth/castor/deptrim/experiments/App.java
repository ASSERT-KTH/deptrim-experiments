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

        // CSV files to be written
        File splDeps = new File("csv/specialized-deps.csv");
        File origDeps = new File("csv/original-deps.csv");
        File deptrimLogs = new File("csv/deptrim-logs.csv");
        File originalBuildLog = new File("csv/original-build-log.csv");
        File specializedBuildLog = new File("csv/specialized-build-log.csv");

        // Write file headers
        FileUtils.writeStringToFile(splDeps, "Project,Dependency,FilePath,Size" + "\n", true);
        FileUtils.writeStringToFile(origDeps, "Project,Dependency,FilePath,Size" + "\n", true);
        FileUtils.writeStringToFile(deptrimLogs, "Project,FilePath,Dependency,NA,RemovedTypes,TotalTypes" + "\n", true);
        FileUtils.writeStringToFile(originalBuildLog, "Project,FilePath,TestRun,Failures,Errors,Skipped" + "\n", true);
        FileUtils.writeStringToFile(specializedBuildLog, "Project,FilePath,TestRun,Failures,Errors,Skipped" + "\n", true);

        try (Stream<Path> filepath = Files.walk(Paths.get("results"))) {
            // process the /deptrim/ directory
            filepath.filter(Files::isRegularFile).forEach(f -> {
                if (f.toString().contains("deptrim/libs-specialized") && f.toString().endsWith(".jar")) {
                    processSpecializedJars(splDeps, f);
                } else if (f.toString().contains("original/compile-scope-dependencies/")) {
                    processCompileScopeJars(origDeps, f);
                } else if (f.toString().endsWith("deptrim/deptrim.log")) {
                    processDeptrimLogs(deptrimLogs, f);
                } else if (f.toString().endsWith("original/maven.log")) {
                    processTestsInBuildLog(originalBuildLog, f);
                } else if (f.toString().endsWith("pom-specialized/maven.log")) {
                    processTestsInBuildLog(specializedBuildLog, f);
                }
            });
        } catch (IOException e) {
            throw new IOException("Directory Not Present!");
        }
    }

    private static void processTestsInBuildLog(File buildLog, Path f) {
        List<String> l = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(f);
            for (String line : lines) {
                if (line.startsWith("Tests run: ")) {
                    String project = f.toString().split("/")[1];
                    String testRun = line.split(" ")[2].substring(0, line.split(" ")[2].length() - 1);
                    String failures = line.split(" ")[4].substring(0, line.split(" ")[4].length() - 1);
                    String errors = line.split(" ")[6].substring(0, line.split(" ")[6].length() - 1);
                    String skipped = line.split(" ")[8];
                    l.add(project + "," + f + "," +
                            testRun + "," +
                            failures + "," +
                            errors + "," +
                            skipped + "\n"
                    );
                } else if (line.startsWith("[INFO] Tests run: ")
                        || line.startsWith("[WARNING] Tests run: ")
                        || line.startsWith("[ERROR] Tests run: ")
                ) {
                    String project = f.toString().split("/")[1];
                    String testRun = line.split(" ")[3].substring(0, line.split(" ")[3].length() - 1);
                    String failures = line.split(" ")[5].substring(0, line.split(" ")[5].length() - 1);
                    String errors = line.split(" ")[7].substring(0, line.split(" ")[7].length() - 1);
                    String skipped = line.split(" ")[9];
                    l.add(project + "," + f + "," +
                            testRun + "," +
                            failures + "," +
                            errors + "," +
                            skipped + "\n"
                    );
                }
            }
            // Write the last test run line
            if (!l.isEmpty()) {
                FileUtils.writeStringToFile(buildLog, l.get(l.size() - 1), true);
            } else {
                FileUtils.writeStringToFile(buildLog, f.toString().split("/")[1] + "," + f + "," +
                        "NA" + "," +
                        "NA" + "," +
                        "NA" + "," +
                        "NA" + "\n", true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processSpecializedJars(File splDeps, Path f) {
        List<String> l = new ArrayList<>();
        String project = f.toString().split("/")[1];
        String depJar = f.toString().split("/")[f.toString().split("/").length - 1];
        l.add(project + "," + depJar + "," + f + "," + FileUtils.sizeOf(new File(f.toString())) + "\n");
        String str = String.join("", l);
        try {
            FileUtils.writeStringToFile(splDeps, str, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processCompileScopeJars(File origDeps, Path f) {
        List<String> l = new ArrayList<>();
        String project = f.toString().split("/")[1];
        String depJar = f.toString().split("/")[f.toString().split("/").length - 1];
        l.add(project + "," + depJar + "," + f + "," + FileUtils.sizeOf(new File(f.toString())) + "\n");
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
                    String project = f.toString().split("/")[1];
                    String specializedDependency = line.split(" ")[3];
                    String nbTypesRemoved = line.split(" ")[5].split("/")[0];
                    String nbTypesTotal = line.split(" ")[5].split("/")[1];
                    l.add(project + "," + f + "," +
                            specializedDependency + "," +
                            nbTypesRemoved + "," + nbTypesTotal + "\n"
                    );
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
