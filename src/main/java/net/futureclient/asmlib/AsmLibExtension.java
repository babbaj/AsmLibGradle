package net.futureclient.asmlib;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.DynamicObjectUtil;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.metaobject.DynamicObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

class AsmLibExtension {

    private final Project project;

    Set<SourceSet> asmLibSourceSets = new HashSet<>();
    Set<String> asmLibMappingConfigs = new HashSet<>();

    public AsmLibExtension(Project project) {
        //TODO: init
        this.project = project;
    }

    public void add(SourceSet sourceSet, String... asmLibMappingConfigs) {
        this.asmLibSourceSets.add(sourceSet);
        this.asmLibMappingConfigs.addAll(Arrays.asList(asmLibMappingConfigs));

        project.afterEvaluate(p -> {
            configure(sourceSet);
        });
    }

    private void configure(SourceSet sourceSet) {
        Task t = project.getTasks().getByName(sourceSet.getCompileJavaTaskName());
        if (!(t instanceof JavaCompile))
            throw new IllegalStateException("Can not add non-java SourceSet (" + sourceSet + ")");
        final JavaCompile compileTask = (JavaCompile) t;

        Path tempDir = getResourcePath(sourceSet);
        Path testFile = tempDir.resolve("test");


        compileTask.doFirst(task -> {
            System.out.println("Preparing asmlib stuff :DD");
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

    private Path getResourcePath(SourceSet sourceSet) {
        return sourceSet.getOutput().getResourcesDir().toPath();
    }

    // convert object to (((groovy))) object
    private DynamicObject asDynamic(Object object) {
        return DynamicObjectUtil.asDynamicObject(object);
    }

}