package net.futureclient.asmlib;

import net.futureclient.asmlib.forgegradle.ForgeGradleVersion;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class AsmLibGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        final ForgeGradleVersion forgeGradleVersion = ForgeGradleVersion.detectForgeGradleVersion(project);

        if (forgeGradleVersion == null)
            throw new InvalidUserDataException("Known ForgeGradle version not found. Make sure ForgeGradle is applied!");
        if (!forgeGradleVersion.isSupported())
            throw new InvalidUserDataException(String.format("Unsupported ForgeGradle version \"%s\"!", forgeGradleVersion.getVersion()));

        project.getExtensions().create("asmlib", AsmLibExtension.class, project, forgeGradleVersion);
    }
}
