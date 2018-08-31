package net.futureclient.asmlib.parser.srg;

import net.futureclient.asmlib.parser.srg.member.FieldMember;
import net.futureclient.asmlib.parser.srg.member.MethodMember;

import java.util.Collections;
import java.util.Map;

public final class SrgMap {

    private final Map<String, String> classMap;
    private final Map<FieldMember, String> fieldMap;
    private final Map<MethodMember, String> methodMap;


    SrgMap(Map<String, String> classes, Map<FieldMember, String> fields, Map<MethodMember, String> methods) {
        this.classMap =  Collections.unmodifiableMap(classes);
        this.fieldMap =  Collections.unmodifiableMap(fields);
        this.methodMap = Collections.unmodifiableMap(methods);
    }


    public Map<String, String> getClassMap() {
        return this.classMap;
    }

    public Map<FieldMember, String> getFieldMap() {
        return this.fieldMap;
    }

    public Map<MethodMember, String> getMethods() {
        return this.methodMap;
    }
}
