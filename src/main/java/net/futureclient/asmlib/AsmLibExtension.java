package net.futureclient.asmlib;

import net.futureclient.asmlib.forgegradle.ForgeGradleVersion;
import net.futureclient.asmlib.parser.srg.SrgMap;
import net.futureclient.asmlib.parser.srg.SrgParser;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

class AsmLibExtension {

    private final Project project;
    private final ForgeGradleVersion forgeGradleVersion;

    private Set<SourceSet> asmLibSourceSets = new HashSet<>();
    private Set<String> asmLibMappingConfigs = new HashSet<>();

    public AsmLibExtension(Project project, ForgeGradleVersion forgeGradleVersion) {
        this.project = project;
        this.forgeGradleVersion = forgeGradleVersion;
    }

    public void add(SourceSet sourceSet, String... asmLibMappingConfigs) {
        this.asmLibSourceSets.add(sourceSet);
        this.asmLibMappingConfigs.addAll(Arrays.asList(asmLibMappingConfigs));

        project.afterEvaluate(p -> this.configure(sourceSet));
    }

    private void configure(SourceSet sourceSet) {
        Task t = project.getTasks().getByName(sourceSet.getCompileJavaTaskName());
        if (!(t instanceof JavaCompile))
            throw new IllegalStateException("Can not add non-java SourceSet (" + sourceSet + ")");
        final JavaCompile compileTask = (JavaCompile) t;

        Path tempDir = this.getResourcePath(sourceSet);
        Path testFile = tempDir.resolve("test");

        compileTask.doFirst(task -> {
            File mcpToNotch;
            File mcpToSrg;

            switch (this.forgeGradleVersion) {
                case FORGEGRADLE_2_X:
                    final Task genSrgs = project.getTasks().getByName("genSrgs");
                    mcpToNotch = (File) genSrgs.property("mcpToNotch");
                    mcpToSrg = (File) genSrgs.property("mcpToSrg");
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported ForgeGradle Version!");
            }

            Objects.requireNonNull(mcpToNotch);
            Objects.requireNonNull(mcpToSrg);

            System.out.println("GenMappingsTask: " + mcpToNotch);
            System.out.println("GenMappingsTask: " + mcpToSrg);

            final SrgMap mcpToNotchMap = parseSrgFile(mcpToNotch);

            System.out.println(mcpToNotchMap.getMethodMap().size());

            final SrgMap mcpToSrgMap = parseSrgFile(mcpToSrg);


            System.out.println(mcpToSrgMap.getMethodMap().size());
        });

        compileTask.doLast(task -> {
            System.out.println("gonna put file here");
            try {
                Files.write(testFile, "hello world".getBytes(), StandardOpenOption.CREATE);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private SrgMap parseSrgFile(File file) {
        try (BufferedReader reader = newReader(file)) {
            return SrgParser.parse(reader.lines());
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private BufferedReader newReader(File file) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    }


    private Path getResourcePath(SourceSet sourceSet) {
        return sourceSet.getOutput().getResourcesDir().toPath();
    }
}