package se.kth.castor.deptrim.experiments;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class DeptrimDataCollector {

    // CSV files to be written
    private static File deptrimResults = new File("csv/deptrim-results.csv");

    public static void main(String[] args) throws IOException {
        // Write file headers
        FileUtils.writeStringToFile(deptrimResults, "Project,FilePath,SpecializedDependencies,RemovedClasses,JarSizeReduction,CompilationPass,TestsPass" + "\n", true);

        try (Stream<Path> filepath = Files.walk(Paths.get("results"))) {
            filepath.filter(Files::isRegularFile).forEach(f -> {
                if (f.toString().endsWith("/depclean/depclean.log")) {
                    try {
                        processPomSpecializedLogs(deptrimResults, f);
                    } catch (
                            Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (
                IOException e) {
            throw new IOException("Directory Not Present!");
        }

    }

    private static void processPomSpecializedLogs(File deptrimResults, Path f) {
    }
}
