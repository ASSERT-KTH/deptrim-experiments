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

public class DeptrimDataCollectorRQ1 {

    // CSV files to be written
    private static File pomSpecializedBuildResultLogs = new File("csv/RQ1/pom-specialized-build-result-logs.csv");
    private static File pomTotallySpecializedBuildResultLogs = new File("csv/RQ1/pom-totally-specialized-build-result-logs.csv");

    public static void main(String[] args) throws IOException {
        // remove files if exists
        if (pomSpecializedBuildResultLogs.exists()) {
            pomSpecializedBuildResultLogs.delete();
        } else if (pomTotallySpecializedBuildResultLogs.exists()) {
            pomTotallySpecializedBuildResultLogs.delete();
        }


        // Write file headers
        FileUtils.writeStringToFile(pomSpecializedBuildResultLogs,
                "Project,FilePath,BuildResult" + "\n", true
        );
        FileUtils.writeStringToFile(pomTotallySpecializedBuildResultLogs,
                "Project,FilePath,BuildResult" + "\n", true
        );

        String regex = "^.*deptrim/pom-specialized_([1-9]|[1-9][0-9]|100)_([1-9]|[1-9][0-9]|100)/maven.log$";
        Pattern pattern = Pattern.compile(regex);

        try (Stream<Path> filepath = Files.walk(Paths.get("results"))) {
            filepath.filter(Files::isRegularFile).forEach(f -> {
                if (f.toString().endsWith("/deptrim/pom-specialized/maven.log")) {
                    processBuildLogs(pomTotallySpecializedBuildResultLogs, f);
                } else if (pattern.matcher(f.toString()).matches()) {
                    processBuildLogs(pomSpecializedBuildResultLogs, f);
                }
            });
        } catch (
                IOException e) {
            throw new IOException("Directory Not Present!");
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


}
