package net.futureclient.asmlib;

import com.google.gson.*;
import net.futureclient.asmlib.forgegradle.ForgeGradleVersion;
import net.futureclient.asmlib.parser.srg.SrgMap;
import net.futureclient.asmlib.parser.srg.SrgParser;
import net.futureclient.asmlib.parser.srg.member.FieldMember;
import net.futureclient.asmlib.parser.srg.member.MethodMember;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AsmLibExtension {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Project project;
    private final ForgeGradleVersion forgeGradleVersion;

    private Set<SourceSet> asmLibSourceSets = new HashSet<>();
    private Set<String> asmLibMappingConfigs = new HashSet<>();

    private File mcpToNotch;
    private File mcpToSrg;

    public AsmLibExtension(Project project, ForgeGradleVersion forgeGradleVersion) {
        this.project = project;
        this.forgeGradleVersion = forgeGradleVersion;
    }

    public void add(SourceSet sourceSet, String... asmLibMappingConfigs) {
        this.asmLibSourceSets.add(sourceSet);
        this.asmLibMappingConfigs.addAll(Arrays.asList(asmLibMappingConfigs));

        project.afterEvaluate(p -> this.configure(sourceSet));
    }

    private void configure(SourceSet sourceSet) {
        Task t = project.getTasks().getByName(sourceSet.getCompileJavaTaskName());
        if (!(t instanceof JavaCompile))
            throw new IllegalStateException("Can not add non-java SourceSet (" + sourceSet + ")");
        final JavaCompile compileTask = (JavaCompile) t;

        Path tempDir = this.getResourcePath(sourceSet);
        Path testFile = tempDir.resolve("test");

        compileTask.doFirst(task -> {

            switch (this.forgeGradleVersion) {
                case FORGEGRADLE_2_X:
                    final Task genSrgs = project.getTasks().getByName("genSrgs");
                    mcpToNotch = (File) genSrgs.property("mcpToNotch");
                    mcpToSrg = (File) genSrgs.property("mcpToSrg");
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported ForgeGradle Version!");
            }

            Objects.requireNonNull(mcpToNotch);
            Objects.requireNonNull(mcpToSrg);

            System.out.println("GenMappingsTask: " + mcpToNotch);
            System.out.println("GenMappingsTask: " + mcpToSrg);
        });

        compileTask.doLast(task -> {
            System.out.println("gonna put file here");

            final String json = serializeJson(parseSrgFile(mcpToNotch), parseSrgFile(mcpToSrg));
            try {
                System.out.println("writing the file...");
                Files.write(testFile, json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private String serializeJson(SrgMap mcpToNotch, SrgMap mcpToSrg) {
        System.out.println("Serializing json");
        final JsonObject root = new JsonObject();
        {
            final JsonObject mappings = new JsonObject();
            root.add("mappings", mappings);
            // map mcp name to notch name
            final Map<String, String> classNames = mcpToNotch.getClassMap();
            System.out.println(classNames.size() + " classes");

            classNames.forEach((mcpClass, obfClass) -> {
                final JsonObject classJson = new JsonObject();
                mappings.add(mcpClass, classJson);

                classJson.add("notch", new JsonPrimitive(obfClass));
                {
                    final JsonObject fields = new JsonObject();
                    classJson.add("fields", fields);

                    addJsonValues(fields, mcpToNotch, mcpToSrg, mcpClass, SrgMap::getFieldMap,
                            FieldMember::getName, FieldMember::getObfName);
                }
                {
                    final JsonObject methods = new JsonObject();
                    classJson.add("methods", methods);

                    addJsonValues(methods, mcpToNotch, mcpToSrg, mcpClass, SrgMap::getMethodMap,
                            MethodMember::getCombinedName, MethodMember::getMappedName);
                }

            });

        }
        return GSON.toJson(root);
    }

    // TODO: use polymorphism instead of passing functions
    private static <T> void addJsonValues(JsonObject json,
                                          SrgMap mcpToNotch, SrgMap mcpToSrg,
                                          String parentClass,
                                          Function<SrgMap, Map<String, Set<T>>> getMap,
                                          Function<T, String> getHeader,
                                          Function<T, String> getProperty)
    {
        final Set<T> notchMethods = getMap.apply(mcpToNotch).get(parentClass);
        final Set<T> seargeMethods = getMap.apply(mcpToSrg).get(parentClass);

        if (notchMethods == null || seargeMethods == null ) return; // this class has no members of type T

        final Map<String, String> notchMap = notchMethods.stream()
                .collect(mapToSelf(getHeader, getProperty));
        final Map<String, String> seargeMap = seargeMethods.stream()
                .collect(mapToSelf(getHeader, getProperty));

        // map mcp name to a map that maps type to the type's mapping
        // (e.g allMembers.get("player").get("notch").equals("c") == true)
        // created by combining the notch and searge member maps
        final Map<String, Map<String, String>> allMembers = new LinkedHashMap<>();
        notchMap.forEach((fullMcp, notch) -> {
            final String searge = seargeMap.get(fullMcp);
            Objects.requireNonNull(searge, "Failed to find searge mapping for member: " + fullMcp); // TODO: check for missing obf members
            final Map<String, String> inner = new LinkedHashMap<>();
            inner.put("notch", notch);
            inner.put("searge", searge);
            allMembers.put(fullMcp, inner);
        });

        allMembers.forEach((fullMcp, map) -> {
            JsonObject values = new JsonObject();
            values.addProperty("notch", map.get("notch"));
            values.addProperty("searge", map.get("searge"));
            json.add(fullMcp, values);
        });

    }


    private static <T, K, V> Collector<T, ?, Map<K, V>> mapToSelf(Function<? super T, ? extends K> keyExtractor,
                                                                  Function<? super T, ? extends V> valueMapper)
    {
        return Collectors.toMap(
                keyExtractor,
                valueMapper,
                (k1, k2) -> {throw new IllegalStateException("Duplicate key: " + k1);},
                LinkedHashMap::new
                );
    }




    private SrgMap parseSrgFile(File file) {
        try (BufferedReader reader = newReader(file)) {
            return SrgParser.parse(reader.lines());
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private BufferedReader newReader(File file) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    }


    private Path getResourcePath(SourceSet sourceSet) {
        return sourceSet.getOutput().getResourcesDir().toPath();
    }
}