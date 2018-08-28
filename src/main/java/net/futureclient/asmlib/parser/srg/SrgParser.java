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

public interface SrgParser {

    // Parses an mcp-notch.srg or mcp-srg.srg file
    static SrgMap parse(Stream<String> lines) {
        final Map<ClassMember, ClassMember> classes = new HashMap<>();
        final Map<FieldMember, FieldMember> fields = new HashMap<>();
        final Map<MethodMember, MethodMember> methods = new HashMap<>();


        lines.forEach(line -> {
            String[] split = line.split(" +");

            switch (split[0]) {
                case "CL:": {
                    // CL: net/minecraft/client/Minecraft bib
                    String mcpName = split[1], obfName = split[2];

                    classes.put(new ClassMember(mcpName), new ClassMember(obfName));
                    break;
                }
                case "FD:": {
                    // FD: net/minecraft/client/Minecraft/player bib/h
                    String mcpName = split[1], obfName = split[2];

                    fields.put(new FieldMember(mcpName), new FieldMember(obfName));
                    break;
                }
                case "MD:": {
                    // MD: net/minecraft/client/Minecraft/getMinecraft ()Lnet/minecraft/client/Minecraft; bib/z ()Lbib;
                    String mcpName = split[1], mcpSignature = split[2], obfName = split[3], obfSignature = split[4];

                    methods.put(new MethodMember(mcpName, mcpSignature), new MethodMember(obfName, obfSignature));
                    break;
                }
            }
        });

        return new SrgMap(classes, fields, methods);
    }




}
