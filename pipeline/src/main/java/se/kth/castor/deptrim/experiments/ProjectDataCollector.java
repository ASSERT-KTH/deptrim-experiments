package se.kth.castor.deptrim.experiments;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class ProjectDataCollector {

    // CSV files to be written
    private static File projectClasses = new File("csv/Descriptive/project-classes.csv");

    private static HashMap<String, List<Dependency>> map = new HashMap<>();

    public static void execute() throws IOException {


        // delete files if they already exist
        if (projectClasses.exists()) {
            FileUtils.forceDelete(projectClasses);
        }

        FileUtils.writeStringToFile(projectClasses,
                "Project,Dependency,DependencyJar,TotalOriginalClasses" + "\n", true
        );

        try (Stream<Path> filepath = Files.walk(Paths.get("results"))) {
            filepath.filter(Files::isRegularFile).forEach(f -> {
                if (f.toString().endsWith("/original/dependency-list.log")) {
                    getOriginalCompileScopeClasses(f);
                }
            });
            // print all the elements in the map
            // for (String key : map.keySet()) {
            //     System.out.println(key + " : " + map.get(key).size());
            // }
        }

        try (Stream<Path> directoryPath = Files.walk(Paths.get("results"))) {
            directoryPath.filter(Files::isDirectory).forEach(f -> {
                if (f.toString().endsWith("/original/compile-scope-dependencies/dependency")) {
                    try {
                        computeClassesInOriginalDepsJars(projectClasses, f);
                    } catch (
                            IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    private static void computeClassesInOriginalDepsJars(File splDeps, Path f) throws IOException {
        String project = f.toString().split("/")[1];
        List<Dependency> dependencies = map.get(project);
        for (Dependency dependency : dependencies) {
            File jar = new File(f.toString() + "/" + dependency.jarName + ".jar");
            if (jar.exists()) {
                FileUtils.writeStringToFile(splDeps, project + "," + dependency.GAV+ "," + dependency.jarName + "," + totalClassesInJar(jar) + "\n", true);
            }
        }
    }

    private static void getOriginalCompileScopeClasses(Path f) {
        String project = f.toString().split("/")[1];
        // System.out.println(project);
        try {
            Scanner sc = new Scanner(new File(f.toString()));
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] parts = line.split(":");
                if ((line.startsWith("[INFO]    ") && line.endsWith(":compile")) || (parts.length > 4 && parts[4].startsWith("compile"))) {

                    String depJar = "";
                    String GAV = "";
                    if(parts.length > 5) {
                        //handle special case: [INFO]    org.yaml:snakeyaml:jar:android:1.26:compile
                        // System.out.println(project);
                        depJar = parts[1] + "-" + parts[4] + "-" + parts[3];
                        GAV =  parts[0].split("    ")[1] + ":" + parts[1] + ":" + parts[4];
                    } else {
                        // normal case: [INFO]    org.apache.commons:commons-lang3:jar:3.5:compile
                        depJar = parts[1] + "-" + parts[3];
                        GAV = parts[0].split("    ")[1] + ":" + parts[1] + ":" + parts[3];
                    }

                    // System.out.println(depJar);
                    if (map.containsKey(project)) {
                        map.get(project).add(new Dependency(GAV, depJar));
                    } else {
                        List<Dependency> l = new ArrayList<>();
                        l.add(new Dependency(GAV, depJar));
                        map.put(project, l);
                    }
                }
            }
            sc.close();
        } catch (
                FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static int totalClassesInJar(File givenFile) throws IOException {
        int totalClasses = 0;
        try (JarFile jarFile = new JarFile(givenFile)) {
            Enumeration<JarEntry> e = jarFile.entries();
            while (e.hasMoreElements()) {
                JarEntry jarEntry = e.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    totalClasses++;
                }
            }
        }
        return totalClasses;
    }

}

class Dependency {
    String GAV;
    String jarName;

    public Dependency(String GAV, String jarName) {
        this.GAV = GAV;
        this.jarName = jarName;
    }

    public String getGAV() {
        return GAV;
    }

    public String getJarName() {
        return jarName;
    }
}



