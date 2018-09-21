package net.futureclient.asmlib.parser.transformer;

import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public Set<String> getReferencedClasses() {
        return getMethods().stream()
                .map(str -> str.replaceAll("^.+(?=\\()", "")) // strip name
                .map(Type::getMethodType)
                .flatMap(type -> Stream.concat(Stream.of(type.getArgumentTypes()), Stream.of(type.getReturnType())))
                .filter(type -> type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)
                .map(this::removeArray)
                .map(Type::getInternalName)
                .collect(Collectors.toSet());
    }

    private Type removeArray(Type type) {
        if (type.getSort() == Type.OBJECT) return type;
        return Type.getObjectType(type.getInternalName().replaceAll("^\\[+", ""));
    }
}
