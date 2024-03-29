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

public class DepcleanDataCollector {

    // CSV files to be written
    private static File originalBuildResultLogs = new File("csv/Descriptive/original-build-result-logs.csv");
    private static File depcleanBuildResultLogs = new File("csv/Descriptive/depclean-build-result-logs.csv");
    private static File originalTestsLogs = new File("csv/Descriptive/original-tests-logs.csv");
    private static File depcleanResults = new File("csv/Descriptive/depclean-results.csv");
    private static File projectClassesSize = new File("csv/Descriptive/project-classes-size.csv");
    private static File projectClassesNumber = new File("csv/Descriptive/project-classes-number.csv");


    public static void execute() throws IOException {

        // delete files if they already exist
        if (originalBuildResultLogs.exists()) {
            FileUtils.forceDelete(originalBuildResultLogs);
        }
        if (depcleanBuildResultLogs.exists()) {
            FileUtils.forceDelete(depcleanBuildResultLogs);
        }
        if (originalTestsLogs.exists()) {
            FileUtils.forceDelete(originalTestsLogs);
        }
        if (depcleanResults.exists()) {
            FileUtils.forceDelete(depcleanResults);
        }
        if (projectClassesSize.exists()) {
            FileUtils.forceDelete(projectClassesSize);
        }
        if (projectClassesNumber.exists()) {
            FileUtils.forceDelete(projectClassesNumber);
        }

        // Write file headers
        FileUtils.writeStringToFile(originalTestsLogs,
                "Project,FilePath,TestRun,TestFailure,TestError,TestSkipped" + "\n", true
        );

        FileUtils.writeStringToFile(originalBuildResultLogs,
                "Project,FilePath,BuildResult" + "\n", true
        );

        FileUtils.writeStringToFile(depcleanBuildResultLogs,
                "Project,FilePath,BuildResult" + "\n", true
        );

        FileUtils.writeStringToFile(depcleanResults, "Project,FilePath," +
                "UsedDirectCompile,UsedTransitiveCompile,UsedInheritedDirectCompile,UsedInheritedTransitiveCompile," +
                "UsedDirectNonCompile,UsedTransitiveNonCompile,UsedInheritedDirectNonCompile,UsedInheritedTransitiveNonCompile," +

                "UnusedDirectCompile,UnusedTransitiveCompile,UnusedInheritedDirectCompile,UnusedInheritedTransitiveCompile," +
                "UnusedDirectNonCompile,UnusedTransitiveNonCompile,UnusedInheritedDirectNonCompile,UnusedInheritedTransitiveNonCompile," +

                "DirectChartCompile,TransitiveChartCompile,InheritedChartCompile" + "\n", true);

        FileUtils.writeStringToFile(projectClassesSize,
                "Project,FilePath,ProjectClassesSize" + "\n", true
        );

        FileUtils.writeStringToFile(projectClassesNumber,
                "Project,FilePath,ProjectClassesNumber" + "\n", true
        );

        try (Stream<Path> filepath = Files.walk(Paths.get("results"))) {
            filepath.filter(Files::isRegularFile).forEach(f -> {

                if (f.toString().endsWith("/depclean/depclean.log")) {
                    try {
                        processDepcleanLogs(depcleanResults, f);
                    } catch (
                            IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (f.toString().endsWith("/deptrim/maven.log")) {
                    processBuildLogs(originalBuildResultLogs, f);
                    processOriginalTestsLogs(originalTestsLogs, f);
                } else if (f.toString().endsWith("/depclean/pom-debloated/maven.log")) {
                    processBuildLogs(depcleanBuildResultLogs, f);
                }
            });

            try (Stream<Path> directoryPath = Files.walk(Paths.get("results"))) {
                directoryPath.filter(Files::isDirectory).forEach(f -> {
                    // System.out.println(f.toFile().getAbsolutePath());
                    if (f.toString().endsWith("/target/classes")) {
                        getProjectClassesSize(projectClassesSize, f);
                        getProjectClassesNumber(projectClassesNumber, f);
                    }
                });
            }
        } catch (
                IOException e) {
            throw new IOException("Directory Not Present!");
        }

    }

    private static void getProjectClassesNumber(File projectClassesNumber, Path f) {
        List<String> l = new ArrayList<>();
        String project = f.toString().split("/")[1];
        int totalClassFiles = countClassFiles(f.toFile());
        l.add(project + "," + f + "," + totalClassFiles + "\n");
        String str = String.join("", l);
        try {
            FileUtils.writeStringToFile(projectClassesNumber, str, true);
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    private static int countClassFiles(File directory) {
        int count = 0;
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                count += countClassFiles(file);
            } else if (file.getName().endsWith(".class")) {
                count++;
            }
        }
        return count;
    }

    private static void getProjectClassesSize(File projectClassesSize, Path f) {
        List<String> l = new ArrayList<>();
        String project = f.toString().split("/")[1];
        double size = FileUtils.sizeOf(new File(f.toString())) / 1024;
        l.add(project + "," + f + "," + size + "\n");
        String str = String.join("", l);
        try {
            FileUtils.writeStringToFile(projectClassesSize, str, true);
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    private static void processBuildLogs(File deptrimResults, Path f) {
        List<String> l = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(f);
            for (String line : lines) {
                if (line.startsWith("[INFO] BUILD SUCCESS")) {
                    String project = f.toString().split("/")[1];
                    String build = "SUCCESS";
                    l.add(project + "," + f + "," + build + "\n");
                } else if (line.startsWith("[INFO] BUILD FAILURE")) {
                    String project = f.toString().split("/")[1];
                    String build = "FAILURE";
                    l.add(project + "," + f + "," + build + "\n");
                }
            }
            // Write the last test run line
            if (!l.isEmpty()) {
                FileUtils.writeStringToFile(deptrimResults, l.get(l.size() - 1), true);
            } else {
                FileUtils.writeStringToFile(deptrimResults, f.toString().split("/")[1] + "," + f + "," +
                        "NA" + "," +
                        "NA" + "," +
                        "NA" + "," +
                        "NA" + "\n", true);
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    private static void processOriginalTestsLogs(File deptrimResults, Path f) {
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
                FileUtils.writeStringToFile(deptrimResults, l.get(l.size() - 1), true);
            } else {
                FileUtils.writeStringToFile(deptrimResults, f.toString().split("/")[1] + "," + f + "," +
                        "NA" + "," +
                        "NA" + "," +
                        "NA" + "," +
                        "NA" + "\n", true);
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }

    }

    private static void processDepcleanLogs(File depcleanResults, Path f) throws IOException {
        List<String> l = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(f);
            String project = f.toString().split("/")[1];

            // used
            int usedDirectCompile = 0;
            int usedTransitiveCompile = 0;
            int usedInheritedDirectCompile = 0;
            int usedInheritedTransitiveCompile = 0;
            // unused
            int unusedDirectCompile = 0;
            int unusedTransitiveCompile = 0;
            int unusedInheritedDirectCompile = 0;
            int unusedInheritedTransitiveCompile = 0;

            // used
            int usedDirectNonCompile = 0;
            int usedTransitiveNonCompile = 0;
            int usedInheritedDirectNonCompile = 0;
            int usedInheritedTransitiveNonCompile = 0;
            // unused
            int unusedDirectNonCompile = 0;
            int unusedTransitiveNonCompile = 0;
            int unusedInheritedDirectNonCompile = 0;
            int unusedInheritedTransitiveNonCompile = 0;

            // used
            boolean isBetweenUsedDirect = false;
            boolean isBetweenUsedTransitive = false;
            boolean isBetweenUsedInheritedDirect = false;
            boolean isBetweenUsedInheritedTransitive = false;
            // unused
            boolean isBetweenUnusedDirect = false;
            boolean isBetweenUnusedTransitive = false;
            boolean isBetweenUnusedInheritedDirect = false;
            boolean isBetweenUnusedInheritedTransitive = false;

            for (String line : lines) {
                if (line.startsWith("USED DIRECT DEPENDENCIES")) {
                    isBetweenUsedDirect = true;
                } else if (line.startsWith("USED TRANSITIVE DEPENDENCIES")) {
                    isBetweenUsedDirect = false;
                    isBetweenUsedTransitive = true;
                } else if (line.startsWith("USED INHERITED DIRECT DEPENDENCIES")) {
                    isBetweenUsedInheritedDirect = true;
                    isBetweenUsedTransitive = false;
                } else if (line.startsWith("USED INHERITED TRANSITIVE DEPENDENCIES")) {
                    isBetweenUsedInheritedTransitive = true;
                    isBetweenUsedInheritedDirect = false;
                } else if (line.startsWith("POTENTIALLY UNUSED DIRECT DEPENDENCIES")) {
                    isBetweenUnusedDirect = true;
                    isBetweenUsedInheritedTransitive = false;
                } else if (line.startsWith("POTENTIALLY UNUSED TRANSITIVE DEPENDENCIES")) {
                    isBetweenUnusedTransitive = true;
                    isBetweenUnusedDirect = false;
                } else if (line.startsWith("POTENTIALLY UNUSED INHERITED DIRECT DEPENDENCIES")) {
                    isBetweenUnusedInheritedDirect = true;
                    isBetweenUnusedTransitive = false;
                } else if (line.startsWith("POTENTIALLY UNUSED INHERITED TRANSITIVE DEPENDENCIES")) {
                    isBetweenUnusedInheritedTransitive = true;
                    isBetweenUnusedInheritedDirect = false;
                }

                if (isBetweenUsedDirect) {
                    if (line.contains(":compile ")) {
                        usedDirectCompile++;
                    } else if (isNonCompileDependency(line)) {
                        usedDirectNonCompile++;
                    }
                } else if (isBetweenUsedTransitive) {
                    if (line.contains(":compile ")) {
                        usedTransitiveCompile++;
                    } else if (isNonCompileDependency(line)) {
                        usedTransitiveNonCompile++;
                    }
                } else if (isBetweenUsedInheritedDirect) {
                    if (line.contains(":compile ")) {
                        usedInheritedDirectCompile++;
                    } else if (isNonCompileDependency(line)) {
                        usedInheritedDirectNonCompile++;
                    }
                } else if (isBetweenUsedInheritedTransitive) {
                    if (line.contains(":compile ")) {
                        usedInheritedTransitiveCompile++;
                    } else if (isNonCompileDependency(line)) {
                        usedInheritedTransitiveNonCompile++;
                    }
                } else if (isBetweenUnusedDirect) {
                    if (line.contains(":compile ")) {
                        unusedDirectCompile++;
                    } else if (isNonCompileDependency(line)) {
                        unusedDirectNonCompile++;
                    }
                } else if (isBetweenUnusedTransitive) {
                    if (line.contains(":compile ")) {
                        unusedTransitiveCompile++;
                    } else if (isNonCompileDependency(line)) {
                        unusedTransitiveNonCompile++;
                    }
                } else if (isBetweenUnusedInheritedDirect) {
                    if (line.contains(":compile ")) {
                        unusedInheritedDirectCompile++;
                    } else if (isNonCompileDependency(line)) {
                        unusedInheritedDirectNonCompile++;
                    }
                } else if (isBetweenUnusedInheritedTransitive) {
                    if (line.contains(":compile ")) {
                        unusedInheritedTransitiveCompile++;
                    } else if (isNonCompileDependency(line)) {
                        unusedInheritedTransitiveNonCompile++;
                    }
                }

            }
            l.add(project + "," + f + "," +
                    // Used
                    usedDirectCompile + "," +
                    usedTransitiveCompile + "," +
                    usedInheritedDirectCompile + "," +
                    usedInheritedTransitiveCompile + "," +

                    usedDirectNonCompile + "," +
                    usedTransitiveNonCompile + "," +
                    usedInheritedDirectNonCompile + "," +
                    usedInheritedTransitiveNonCompile + "," +

                    // Unused
                    unusedDirectCompile + "," +
                    unusedTransitiveCompile + "," +
                    unusedInheritedDirectCompile + "," +
                    unusedInheritedTransitiveCompile + "," +

                    unusedDirectNonCompile + "," +
                    unusedTransitiveNonCompile + "," +
                    unusedInheritedDirectNonCompile + "," +
                    unusedInheritedTransitiveNonCompile + "," +

                    "\\ChartSmall[r]{" + usedDirectCompile + "}{" + Math.addExact(usedDirectCompile, unusedDirectCompile) + "}" + "," +
                    "\\ChartSmall[r]{" + usedTransitiveCompile + "}{" + Math.addExact(usedTransitiveCompile, unusedTransitiveCompile) + "}" + "," +
                    "\\ChartSmall[r]{" + Math.addExact(usedInheritedDirectCompile, usedInheritedTransitiveCompile) + "}{" + Math.addExact(Math.addExact(Math.addExact(usedInheritedDirectCompile, unusedInheritedDirectCompile), usedInheritedTransitiveCompile), unusedInheritedTransitiveCompile) + "}" + "\n");
        } catch (
                IOException e) {
            System.out.println("Error reading file " + f);

        }
        FileUtils.writeStringToFile(depcleanResults, l.get(l.size() - 1), true);
    }

    private static int getValue(String line) {
        return Integer.parseInt(line.split("\\[")[1].split("]")[0]);
    }

    private static boolean isNonCompileDependency(String line) {
        return line.contains(":provided ") || line.contains(":runtime ") || line.contains(":test ") || line.contains(":system ") || line.contains(":import ");
    }


}
