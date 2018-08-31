package net.futureclient.asmlib.parser.srg;

import javafx.util.Pair;
import net.futureclient.asmlib.parser.srg.member.FieldMember;
import net.futureclient.asmlib.parser.srg.member.MethodMember;


import java.util.*;
import java.util.stream.Stream;

public abstract class SrgParser {
    private SrgParser() {}

    // Parses an mcp-notch.srg or mcp-srg.srg file
    public static SrgMap parse(Stream<String> lines) {
        final Map<String, String> classes = new LinkedHashMap<>();
        final Map<FieldMember, String> fields = new LinkedHashMap<>();
        final Map<MethodMember, String> methods = new LinkedHashMap<>(); // TODO: use set


        lines.forEach(line -> {
            String[] split = line.split(" +");

            switch (split[0]) {
                case "CL:": {
                    // CL: net/minecraft/client/Minecraft bib
                    String mcpName = split[1], obfName = split[2];

                    classes.put(mcpName, obfName);
                    break;
                }
                case "FD:": {
                    // FD: net/minecraft/client/Minecraft/player bib/h
                    String fullMCpName = split[1], fullObfName = split[2];
                    final Pair<String, String> mcpName = splitClassAndName(fullMCpName);
                    final Pair<String, String> obfName = splitClassAndName(fullObfName);
                    fields.put(new FieldMember(mcpName.getKey(), mcpName.getValue()), obfName.getValue());
                    break;
                }
                case "MD:": {
                    // MD: net/minecraft/client/Minecraft/getMinecraft ()Lnet/minecraft/client/Minecraft; bib/z ()Lbib;
                    String fullMcpName = split[1], mcpSignature = split[2], fullObfName = split[3], obfSignature = split[4];
                    final String obfName = splitClassAndName(fullObfName).getValue();
                    final Pair<String, String> mcpName = splitClassAndName(fullMcpName);


                    methods.put(new MethodMember(mcpName.getValue(), mcpSignature, mcpName.getKey()), obfName);
                    break;
                }
            }
        });
        return new SrgMap(classes, fields, methods);
    }

    private static Pair<String, String> splitClassAndName(String str) {
        final int lastSlash = str.lastIndexOf('/');
        final String clazz = str.substring(0, lastSlash);
        final String name = str.substring(lastSlash + 1);
        return new Pair<>(clazz, name);
    }


}
