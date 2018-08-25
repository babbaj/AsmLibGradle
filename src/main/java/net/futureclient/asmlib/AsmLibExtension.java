package net.futureclient.asmlib;

import org.gradle.api.Project;

class AsmLibExtension {

    private Project project;

    AsmLibExtension(Project project) {
        this.project = project;
        this.project.afterEvaluate(p -> System.out.println("epic"));
    }
}
