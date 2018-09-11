package net.futureclient.asmlib;

import java.util.Collections;
import java.util.Set;

public final class TransformerInfo {

    private final String className;
    private final Set<String> fields;
    private final Set<String> methods;

    public TransformerInfo(String className, Set<String> fields, Set<String> methods) {
        this.className = className;
        this.fields = Collections.unmodifiableSet(fields);
        this.methods = Collections.unmodifiableSet(methods);
    }

    public String getClassName() {
        return this.className;
    }

    public Set<String> getFields() {
        return this.fields;
    }

    public Set<String> getMethods() {
        return this.methods;
    }
}
