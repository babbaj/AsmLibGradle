package net.futureclient.asmlib.parser.srg;

import net.futureclient.asmlib.parser.srg.member.FieldMember;
import net.futureclient.asmlib.parser.srg.member.MethodMember;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class SrgMap extends ClassInfoMap<FieldMember, MethodMember> {

    // map mcp to notch
    private final Map<String, String> classMap;


    SrgMap(Map<String, String> classes, Map<String, Set<FieldMember>> fields, Map<String, Set<MethodMember>> methods) {
        super(fields, methods);
        this.classMap =  Collections.unmodifiableMap(classes);
    }


    public Map<String, String> getClassMap() {
        return this.classMap;
    }
}
