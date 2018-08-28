package net.futureclient.asmlib.parser.srg.member;

public class MethodMember {

    private final String name;
    private final String signature;

    public MethodMember(String name, String signature) {
        this.name = name;
        this.signature = signature;
    }

    public String getName() {
        return this.name;
    }

    public String getSignature() {
        return this.signature;
    }
}
