package net.futureclient.asmlib.parser.srg;

import net.futureclient.asmlib.parser.srg.member.FieldMember;
import net.futureclient.asmlib.parser.srg.member.MethodMember;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class SrgMap {

    // map mcp to notch
    private final Map<String, String> classMap;

    // map mcp class name to obf/srg
    private final Map<String, Set<FieldMember>> fieldMap;
    private final Map<String, Set<MethodMember>> methodMap;


    SrgMap(Map<String, String> classes, Map<String, Set<FieldMember>> fields, Map<String, Set<MethodMember>> methods) {
        this.classMap =  Collections.unmodifiableMap(classes);
        this.fieldMap =  Collections.unmodifiableMap(fields);
        this.methodMap = Collections.unmodifiableMap(methods);
    }


    public Map<String, String> getClassMap() {
        return this.classMap;
    }

    public Map<String, Set<FieldMember>> getFieldMap() {
        return this.fieldMap;
    }

    public Map<String, Set<MethodMember>> getMethodMap() {
        return this.methodMap;
    }
}
