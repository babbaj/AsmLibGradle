package net.futureclient.asmlib;

import java.util.Collections;
import java.util.Set;

public class ProjectEntry {

    public final String refmap;
    public final Set<String> configs;

    public ProjectEntry(String refmap, Set<String> configs) {
        this.refmap = refmap;
        this.configs = Collections.unmodifiableSet(configs);
    }
}
