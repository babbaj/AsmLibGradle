package net.futureclient.asmlib;

import net.futureclient.asmlib.forgegradle.ForgeGradleVersion;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import javax.annotation.Nullable;

public class AsmLibGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        final ForgeGradleVersion forgeGradleVersion = this.detectForgeGradleVersion(project);

        if (forgeGradleVersion == null)
            throw new InvalidUserDataException("Known ForgeGradle version not found. Make sure ForgeGradle is applied!");
        if (!forgeGradleVersion.isSupported())
            throw new InvalidUserDataException(String.format("Unsupported ForgeGradle version \"%s\"!", forgeGradleVersion.getVersion()));


        final Task genMappings = project.getTasks().create("genMappings", GenMappingsTask.class);

        project.getTasks().getByName("build").dependsOn(genMappings);


        project.getExtensions().create("asmlib", AsmLibExtension.class, project);
    }

    private @Nullable ForgeGradleVersion detectForgeGradleVersion(Project project) {
        if (project.getTasks().findByName("genSrgs") != null) {
            if (project.getTasks().findByName("reobf") != null)
                return ForgeGradleVersion.FORGEGRADLE_1_X;
            else if (project.getExtensions().findByName("reobf") != null)
                return ForgeGradleVersion.FORGEGRADLE_2_X;
        }

        return null;
    }
}
