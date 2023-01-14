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
    private static File depcleanResults = new File("csv/depclean-results.csv");

    public static void main(String[] args) throws IOException {
        // Write file headers
        FileUtils.writeStringToFile(depcleanResults, "Project,FilePath," +
                "UsedDirectCompile,UsedTransitiveCompile,UsedInheritedDirectCompile,UsedInheritedTransitiveCompile," +
                "UsedDirectNonCompile,UsedTransitiveNonCompile,UsedInheritedDirectNonCompile,UsedInheritedTransitiveNonCompile," +

                "UnusedDirectCompile,UnusedTransitiveCompile,UnusedInheritedDirectCompile,UnusedInheritedTransitiveCompile," +
                "UnusedDirectNonCompile,UnusedTransitiveNonCompile,UnusedInheritedDirectNonCompile,UnusedInheritedTransitiveNonCompile," +

                "DirectChartCompile,TransitiveChartCompile,InheritedDirectChartCompile,InheritedTransitiveChartCompile" +
                "DirectChartNonCompile,TransitiveChartNonCompile,InheritedDirectChartNonCompile,InheritedTransitiveChartNonCompile" + "\n", true);

        try (Stream<Path> filepath = Files.walk(Paths.get("results"))) {
            filepath.filter(Files::isRegularFile).forEach(f -> {
                if (f.toString().endsWith("/depclean/depclean.log")) {
                    try {
                        processDepcleanLogs(depcleanResults, f);
                    } catch (
                            IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (
                IOException e) {
            throw new IOException("Directory Not Present!");
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
                    } else if (line.contains(":provided ") || line.contains(":runtime ") || line.contains(":test ")) {
                        usedDirectNonCompile++;
                    }
                } else if (isBetweenUsedTransitive) {
                    if (line.contains(":compile ")) {
                        usedTransitiveCompile++;
                    } else if (line.contains(":provided ") || line.contains(":runtime ") || line.contains(":test ")) {
                        usedTransitiveNonCompile++;
                    }
                } else if (isBetweenUsedInheritedDirect) {
                    if (line.contains(":compile ")) {
                        usedInheritedDirectCompile++;
                    } else if (line.contains(":provided ") || line.contains(":runtime ") || line.contains(":test ")) {
                        usedInheritedDirectNonCompile++;
                    }
                } else if (isBetweenUsedInheritedTransitive) {
                    if (line.contains(":compile ")) {
                        usedInheritedTransitiveCompile++;
                    } else if (line.contains(":provided ") || line.contains(":runtime ") || line.contains(":test ")) {
                        usedInheritedTransitiveNonCompile++;
                    }
                } else if (isBetweenUnusedDirect) {
                    if (line.contains(":compile ")) {
                        unusedDirectCompile++;
                    } else if (line.contains(":provided ") || line.contains(":runtime ") || line.contains(":test ")) {
                        unusedDirectNonCompile++;
                    }
                } else if (isBetweenUnusedTransitive) {
                    if (line.contains(":compile ")) {
                        unusedTransitiveCompile++;
                    } else if (line.contains(":provided ") || line.contains(":runtime ") || line.contains(":test ")) {
                        unusedTransitiveNonCompile++;
                    }
                } else if (isBetweenUnusedInheritedDirect) {
                    if (line.contains(":compile ")) {
                        unusedInheritedDirectCompile++;
                    } else if (line.contains(":provided ") || line.contains(":runtime ") || line.contains(":test ")) {
                        unusedInheritedDirectNonCompile++;
                    }
                } else if (isBetweenUnusedInheritedTransitive) {
                    if (line.contains(":compile ")) {
                        unusedInheritedTransitiveCompile++;
                    } else if (line.contains(":provided ") || line.contains(":runtime ") || line.contains(":test ")) {
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

                    "\\ChartSmall{" + usedDirectCompile + "}{" + Math.addExact(usedDirectCompile, unusedDirectCompile) + "}" + "," +
                    "\\ChartSmall{" + usedTransitiveCompile + "}{" + Math.addExact(usedTransitiveCompile, unusedTransitiveCompile) + "}" + "," +
                    "\\ChartSmall{" + usedInheritedDirectCompile + "}{" + Math.addExact(usedInheritedDirectCompile, unusedInheritedDirectCompile) + "}" + "," +
                    "\\ChartSmall{" + usedInheritedTransitiveCompile + "}{" + Math.addExact(usedInheritedTransitiveCompile, unusedInheritedTransitiveCompile) + "}" + "," +


                    "\\ChartSmall{" + usedDirectNonCompile + "}{" + Math.addExact(usedDirectNonCompile, unusedDirectNonCompile) + "}" + "," +
                    "\\ChartSmall{" + usedTransitiveNonCompile + "}{" + Math.addExact(usedTransitiveNonCompile, unusedTransitiveNonCompile) + "}" + "," +
                    "\\ChartSmall{" + usedInheritedDirectNonCompile + "}{" + Math.addExact(usedInheritedDirectNonCompile, unusedInheritedDirectNonCompile) + "}" + "," +
                    "\\ChartSmall{" + usedInheritedTransitiveNonCompile + "}{" + Math.addExact(usedInheritedTransitiveNonCompile, unusedInheritedTransitiveNonCompile) + "}" + "," + "\n"
            );
        } catch (
                IOException e) {
            System.out.println("Error reading file " + f);

        }
        FileUtils.writeStringToFile(depcleanResults, l.get(l.size() - 1), true);
    }

    private static int getValue(String line) {
        return Integer.parseInt(line.split("\\[")[1].split("]")[0]);
    }


}
