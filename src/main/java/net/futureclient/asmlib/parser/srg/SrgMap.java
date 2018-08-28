package net.futureclient.asmlib.parser.srg;

import net.futureclient.asmlib.parser.srg.member.ClassMember;
import net.futureclient.asmlib.parser.srg.member.FieldMember;
import net.futureclient.asmlib.parser.srg.member.MethodMember;

import java.util.Collections;
import java.util.Map;

public final class SrgMap {

    private final Map<ClassMember, ClassMember> classMap;
    private final Map<FieldMember, FieldMember> fieldMap;
    private final Map<MethodMember, MethodMember> methodMap;


    SrgMap(Map<ClassMember, ClassMember> classes, Map<FieldMember, FieldMember> fields, Map<MethodMember, MethodMember> methods) {
        this.classMap =  Collections.unmodifiableMap(classes);
        this.fieldMap =  Collections.unmodifiableMap(fields);
        this.methodMap = Collections.unmodifiableMap(methods);
    }


    public Map<ClassMember, ClassMember> getClassMap() {
        return this.classMap;
    }

    public Map<FieldMember, FieldMember> getFieldMap() {
        return this.fieldMap;
    }

    public Map<MethodMember, MethodMember> getMethodMap() {
        return this.methodMap;
    }
}
