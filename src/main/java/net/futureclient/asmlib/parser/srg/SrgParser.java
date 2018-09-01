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
        final Map<String, Set<FieldMember>> fields = new LinkedHashMap<>();
        final Map<String, Set<MethodMember>> methods = new LinkedHashMap<>();


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

                    merge(fields, mcpName.getKey(), new FieldMember(mcpName.getValue(), obfName.getValue()));
                    break;
                }
                case "MD:": {
                    // MD: net/minecraft/client/Minecraft/getMinecraft ()Lnet/minecraft/client/Minecraft; bib/z ()Lbib;
                    String fullMcpName = split[1], mcpSignature = split[2], fullObfName = split[3], obfSignature = split[4];
                    final String obfName = splitClassAndName(fullObfName).getValue();
                    final Pair<String, String> mcpName = splitClassAndName(fullMcpName);

                    merge(methods, mcpName.getKey(), new MethodMember(mcpName.getValue(), mcpSignature, obfName));
                    break;
                }
            }
        });
        return new SrgMap(classes, fields, methods);
    }

    private static <K, V> void merge(Map<K, Set<V>> map, K key, V newValue) {
        map.merge(key,
                new LinkedHashSet<>(Collections.singleton(newValue)),
                (s1, s2) -> {
                    s1.addAll(s2);
                    return s1;
                });
    }

    private static Pair<String, String> splitClassAndName(String str) {
        final int lastSlash = str.lastIndexOf('/');
        final String clazz = str.substring(0, lastSlash);
        final String name = str.substring(lastSlash + 1);
        return new Pair<>(clazz, name);
    }


}
