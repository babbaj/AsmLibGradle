package net.futureclient.asmlib.forgegradle;

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
}
