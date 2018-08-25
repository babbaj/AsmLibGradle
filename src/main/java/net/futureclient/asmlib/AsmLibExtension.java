package net.futureclient.asmlib;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class AsmLibExtension {

    private List<SourceSet> asmLibSourceSets = new ArrayList<>();
    private List<String> asmLibMappingConfigs = new ArrayList<>();

    public AsmLibExtension(Project project) {
        //TODO: init
    }

    public void add(SourceSet sourceSet, String... asmLibMappingConfigs) {
        this.asmLibSourceSets.add(sourceSet);
        this.asmLibMappingConfigs.addAll(Arrays.asList(asmLibMappingConfigs));
    }
}