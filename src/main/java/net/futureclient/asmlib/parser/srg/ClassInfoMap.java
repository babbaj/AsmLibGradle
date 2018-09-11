package net.futureclient.asmlib.parser.srg;

import net.futureclient.asmlib.parser.srg.member.FieldMember;
import net.futureclient.asmlib.parser.srg.member.MethodMember;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 * @param <F> type to represent fields
 * @param <M> type to represent methods
 */
public abstract class ClassInfoMap<F, M> {

    // map mcp class name to obf/srg
    private final Map<String, Set<F>> fieldMap;
    private final Map<String, Set<M>> methodMap;


    ClassInfoMap(Map<String, Set<F>> fields, Map<String, Set<M>> methods) {
        this.fieldMap =  fields;
        this.methodMap = methods;
    }


    public Map<String, Set<F>> getFieldMap() {
        return this.fieldMap;
    }

    public Map<String, Set<M>> getMethodMap() {
        return this.methodMap;
    }
}
