package net.futureclient.asmlib.parser.srg;

import net.futureclient.asmlib.parser.srg.member.ClassMember;
import net.futureclient.asmlib.parser.srg.member.FieldMember;
import net.futureclient.asmlib.parser.srg.member.MethodMember;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class SrgParser {

    private final Map<ClassMember, ClassMember> classMcpToObfMap = new HashMap<>();
    private final Map<FieldMember, FieldMember> fieldMcpToObfMap = new HashMap<>();
    private final Map<MethodMember, MethodMember> methodMcpToObfMap = new HashMap<>();

    public SrgParser(InputStream inputStream) {
        Stream<String> lines = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines();

        lines.forEach(line -> {
            String[] split = line.split(" +");

            switch (split[0]) {
                case "CL:": {
                    String mcpName = split[1], obfName = split[2];

                    this.classMcpToObfMap.put(new ClassMember(mcpName), new ClassMember(obfName));
                    break;
                }
                case "FD:": {
                    String mcpName = split[1], obfName = split[2];

                    this.fieldMcpToObfMap.put(new FieldMember(mcpName), new FieldMember(obfName));
                    break;
                }
                case "MD:": {
                    String mcpName = split[1], mcpSignature = split[2], obfName = split[3], obfSignature = split[4];

                    this.methodMcpToObfMap.put(new MethodMember(mcpName, mcpSignature), new MethodMember(obfName, obfSignature));
                    break;
                }
            }
        });
    }

    public Map<ClassMember, ClassMember> getClassMcpToObfMap() {
        return this.classMcpToObfMap;
    }

    public Map<FieldMember, FieldMember> getFieldMcpToObfMap() {
        return this.fieldMcpToObfMap;
    }

    public Map<MethodMember, MethodMember> getMethodMcpToObfMap() {
        return this.methodMcpToObfMap;
    }
}
