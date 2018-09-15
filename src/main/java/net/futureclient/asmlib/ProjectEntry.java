package net.futureclient.asmlib;

import java.util.Collections;
import java.util.Set;

public class ProjectEntry {

    public final String mappingFile;
    public final Set<String> configs;

    public ProjectEntry(String mappingFile, Set<String> configs) {
        this.mappingFile = mappingFile;
        this.configs = Collections.unmodifiableSet(configs);
    }
}
