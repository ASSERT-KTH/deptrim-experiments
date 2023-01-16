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


public class DeptrimDataCollectorRQ2 {

    // CSV files to be written
    private static File removedAndTotalClassesSpecializedDependency = new File("csv/RQ2/removed-classes-per-specialized-dependency.csv");
    private static File dependencyJarSizeOriginal = new File("csv/RQ2/dependency-jar-size-original.csv");
    private static File dependencyJarSizeSpecialized = new File("csv/RQ2/dependency-jar-size-specialized.csv");

    public static void main(String[] args) throws Exception {

        // Write file headers
        FileUtils.writeStringToFile(removedAndTotalClassesSpecializedDependency, "Project,FilePath,Dependency,RemovedClasses,TotalClasses" + "\n", true);
        FileUtils.writeStringToFile(dependencyJarSizeSpecialized, "Project,FilePath,Dependency,SizeSpecialized" + "\n", true);
        FileUtils.writeStringToFile(dependencyJarSizeOriginal, "Project,FilePath,Dependency,SizeOriginal" + "\n", true);

        String regex = "^.*deptrim/pom-specialized_([1-9]|[1-9][0-9]|100)_([1-9]|[1-9][0-9]|100)/maven.log$";
        Pattern pattern = Pattern.compile(regex);

        try (Stream<Path> filepath = Files.walk(Paths.get("results"))) {
            filepath.filter(Files::isRegularFile).forEach(f -> {
                if (f.toString().endsWith("deptrim/deptrim.log")) {
                    getRemovedClassesFromDeptrimLogs(removedAndTotalClassesSpecializedDependency, f);
                } else if (f.toString().contains("deptrim/libs-specialized") && f.toString().endsWith(".jar")) {
                    computeSizeOfSpecializedJars(dependencyJarSizeSpecialized, f);
                } else if (f.toString().contains("original/compile-scope-dependencies/")) {
                    computeSizeOfOriginalJars(dependencyJarSizeOriginal, f);
                }
            });
        }
    }


    private static void getRemovedClassesFromDeptrimLogs(File depLogs, Path f) {
        List<String> l = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(f);
            for (String line : lines) {
                if (line.contains("[INFO] Specializing dependency ")) {
                    String project = f.toString().split("/")[1];
                    String specializedDependency = line.split(" ")[3].substring(0, line.split(" ")[3].length() - 1);
                    String nbTypesRemoved = line.split(" ")[5].split("/")[0];
                    String nbTypesTotal = line.split(" ")[5].split("/")[1];
                    l.add(project + "," + f + "," + specializedDependency + "," + nbTypesRemoved + "," + nbTypesTotal + "\n");
                }
            }
            String str = String.join("", l);
            try {
                FileUtils.writeStringToFile(depLogs, str, true);
            } catch (
                    IOException e) {
                e.printStackTrace();
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    private static void computeSizeOfSpecializedJars(File splDeps, Path f) {
        List<String> l = new ArrayList<>();
        String project = f.toString().split("/")[1];
        String depJar = f.toString().split("/")[f.toString().split("/").length - 1];
        l.add(project + "," + f + "," + depJar + "," + FileUtils.sizeOf(new File(f.toString())) + "\n");
        String str = String.join("", l);
        try {
            FileUtils.writeStringToFile(splDeps, str, true);
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    private static void computeSizeOfOriginalJars(File origDeps, Path f) {
        List<String> l = new ArrayList<>();
        String project = f.toString().split("/")[1];
        String depJar = f.toString().split("/")[f.toString().split("/").length - 1];
        l.add(project + "," + f + "," + depJar + "," + FileUtils.sizeOf(new File(f.toString())) + "\n");
        String str = String.join("", l);
        try {
            FileUtils.writeStringToFile(origDeps, str, true);
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }


}
