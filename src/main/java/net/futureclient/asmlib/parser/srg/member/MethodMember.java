package net.futureclient.asmlib.parser.srg.member;

import java.util.Objects;

public class MethodMember {

    private final String mcpName;
    private final String signature;

    private final String obfName;


    public MethodMember(String name, String signature, String obfName) {
        this.mcpName = name;
        this.signature = signature;
        this.obfName = obfName;
    }

    public String getMcpName() {
        return this.mcpName;
    }

    public String getSignature() {
        return this.signature;
    }

    public String getCombinedName() {
        return getMcpName() + getSignature();
    }

    public String getMappedName() {
        return this.obfName;
    }


    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other instanceof MethodMember) {
            MethodMember otherMember = (MethodMember)other;
            return this.mcpName.equals(otherMember.mcpName) && this.signature.equals(otherMember.signature);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.mcpName, this.signature);
    }
}
