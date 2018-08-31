package net.futureclient.asmlib.parser.srg.member;

public class FieldMember {

    private final String parentClassName;
    private final String name;

    public FieldMember(String parentClass, String name) {
        this.parentClassName = parentClass;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getParentClass() {
        return this.parentClassName;
    }
}
