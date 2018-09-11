package net.futureclient.asmlib.parser.srg;

import java.util.Map;
import java.util.Set;

public class BasicClassInfoMap extends ClassInfoMap<String, String> {

    private final Set<String> classes;

    public BasicClassInfoMap(Map<String, Set<String>> fields, Map<String, Set<String>> methods, Set<String> classes) {
        super(fields, methods);
        this.classes = classes;
    }

    public Set<String> getClasses() {
        return this.classes;
    }
}
