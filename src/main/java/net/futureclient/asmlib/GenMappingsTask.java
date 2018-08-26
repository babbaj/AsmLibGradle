package net.futureclient.asmlib;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Objects;

// the task responsible for parsing the .srg files and generating a json file for remapping
// TODO: dont want this to be a task
public class GenMappingsTask extends DefaultTask {

    private final Task genSrgs;

    public GenMappingsTask() {
        this.genSrgs = getProject().getTasks().findByName("genSrgs");
        Objects.requireNonNull(genSrgs);
    }

    @TaskAction
    public void runTask() {
        // TODO: properly check if these properties/files exist
        final File mapToNotch = (File)genSrgs.property("mcpToNotch");
        final File mcpToSrg   = (File)genSrgs.property("mcpToSrg");

        System.out.println("GenMappingsTask: " + mapToNotch);
        System.out.println("GenMappingsTask: " + mcpToSrg);
        }


}
