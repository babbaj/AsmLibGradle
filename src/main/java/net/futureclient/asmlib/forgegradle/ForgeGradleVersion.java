package net.futureclient.asmlib.forgegradle;

import org.gradle.api.Project;

public enum ForgeGradleVersion {
    FORGEGRADLE_1_X("1.x", false),
    FORGEGRADLE_2_X("2.x", true),
    FORGEGRADLE_3_X("3.x", false);

    private final String version;
    private final boolean supported;

    ForgeGradleVersion(final String version, final boolean supported) {
        this.version = version;
        this.supported = supported;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean isSupported() {
        return this.supported;
    }

    //TODO: FG 1.x, FG 3.x
    public static ForgeGradleVersion detectForgeGradleVersion(Project project) {
        if (project.getTasks().findByName("genSrgs") != null && project.getExtensions().findByName("reobf") != null)
            return ForgeGradleVersion.FORGEGRADLE_2_X;

        return null;
    }
}
