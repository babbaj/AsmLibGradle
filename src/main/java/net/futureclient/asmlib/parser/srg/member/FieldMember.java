package net.futureclient.asmlib.parser.srg.member;

public class FieldMember {

    private final String name;
    private final String obfName;

    public FieldMember(String name, String obfName) {
        this.name = name;
        this.obfName = obfName;
    }

    public String getName() {
        return this.name;
    }

    public String getObfName() {
        return this.obfName;
    }

}
