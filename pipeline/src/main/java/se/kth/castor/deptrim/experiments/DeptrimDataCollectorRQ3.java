package se.kth.castor.deptrim.experiments;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class DeptrimDataCollectorRQ3 {

    // CSV files to be written
    private static File specializationFailures = new File("csv/RQ3/specialization-failures.csv");

    public static void execute() throws IOException {
        // remove files if exists
        if (specializationFailures.exists()) {
            FileUtils.forceDelete(specializationFailures);
        }


        FileUtils.writeStringToFile(specializationFailures,
                "Project,FilePath,CompilationError,TestRun,TestFailure,TestError,TestSkipped" + "\n", true
        );

        String regex = "^.*deptrim/pom-specialized_([1-9]|[1-9][0-9]|100)_([1-9]|[1-9][0-9]|100)/maven.log$";
        Pattern pattern = Pattern.compile(regex);

        try (Stream<Path> filepath = Files.walk(Paths.get("results"))) {
            filepath.filter(Files::isRegularFile).forEach(f -> {
                if (pattern.matcher(f.toString()).matches()) {
                    processSpecializationFailuresInBuildLogs(specializationFailures, f);
                }
            });
        } catch (
                IOException e) {
            throw new IOException("Directory Not Present!");
        }
    }

    private static void processSpecializationFailuresInBuildLogs(File specializationFailures, Path f) {
        List<String> l = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(f);
            boolean compilationError = true;
            for (String line : lines) {
                String project = f.toString().split("/")[1];

                if (line.startsWith("Tests run: ")) {
                    String testRun = line.split(" ")[2].substring(0, line.split(" ")[2].length() - 1);
                    String failures = line.split(" ")[4].substring(0, line.split(" ")[4].length() - 1);
                    String errors = line.split(" ")[6].substring(0, line.split(" ")[6].length() - 1);
                    String skipped = line.split(" ")[8];
                    l.add(project + "," + f + "," +
                            "no" + "," +
                            testRun + "," +
                            failures + "," +
                            errors + "," +
                            skipped + "\n"
                    );
                    compilationError = false;
                } else if (line.startsWith("[INFO] Tests run: ")
                        || line.startsWith("[WARNING] Tests run: ")
                        || line.startsWith("[ERROR] Tests run: ")
                ) {
                    String testRun = line.split(" ")[3].substring(0, line.split(" ")[3].length() - 1);
                    String failures = line.split(" ")[5].substring(0, line.split(" ")[5].length() - 1);
                    String errors = line.split(" ")[7].substring(0, line.split(" ")[7].length() - 1);
                    String skipped = line.split(" ")[9];
                    if (line.split(" ")[9].endsWith(",")) {
                        skipped = line.split(" ")[9].substring(0, line.split(" ")[9].length() - 1);

                    }
                    l.add(project + "," + f + "," +
                            "no" + "," +
                            testRun + "," +
                            failures + "," +
                            errors + "," +
                            skipped + "\n"
                    );
                    compilationError = false;
                }
            }
            if (compilationError) {
                String project = f.toString().split("/")[1];
                l.add(project + "," + f + "," +
                        "yes" + "," +
                        "0" + "," +
                        "0" + "," +
                        "0" + "," +
                        "0" + "\n"
                );
            }
            // Write the last test run line
            if (!l.isEmpty()) {
                FileUtils.writeStringToFile(specializationFailures, l.get(l.size() - 1), true);
            } else {
                FileUtils.writeStringToFile(specializationFailures, f.toString().split("/")[1] + "," + f + "," +
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
}
