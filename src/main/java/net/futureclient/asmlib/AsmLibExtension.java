package net.futureclient.asmlib;

import com.google.gson.*;
import net.futureclient.asmlib.forgegradle.ForgeGradleVersion;
import net.futureclient.asmlib.parser.srg.SrgMap;
import net.futureclient.asmlib.parser.srg.SrgParser;
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

class AsmLibExtension {

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

            // TODO: This REALLY needs to be optimized
            classNames.forEach((mcpClass, obfClass) -> {
                final JsonObject classJson = new JsonObject();
                mappings.add(mcpClass, classJson);

                classJson.add("notch", new JsonPrimitive(obfClass));
                {
                    final JsonObject fields = new JsonObject();
                    classJson.add("fields", fields);


                    mcpToNotch.getFieldMap().entrySet().stream()
                            .filter(entry -> entry.getKey().getParentClass().equals(mcpClass))
                            .forEach(entry -> {
                        JsonObject fieldValues = new JsonObject();
                        fieldValues.addProperty("notch", entry.getValue());
                        fields.add(entry.getKey().getName(), fieldValues);
                    });
                    mcpToSrg.getFieldMap().entrySet().stream()
                            .filter(entry -> entry.getKey().getParentClass().equals(mcpClass))
                            .forEach(entry -> {
                                JsonObject fieldValues = Optional.ofNullable(fields.get(entry.getKey().getName()))
                                        .map(JsonElement::getAsJsonObject)
                                        .orElseThrow(() -> new IllegalStateException("Missing obf field: " + entry.getKey().getName())); // TODO: check for missing srg fields
                                fieldValues.addProperty("srg", entry.getValue());
                                fields.add(entry.getKey().getName(), fieldValues);
                            });

                }
                {
                    final JsonObject methods = new JsonObject();
                    classJson.add("methods", methods);

                    mcpToNotch.getMethods().entrySet().stream()
                            .filter(entry -> entry.getKey().getParentClass().equals(mcpClass))
                            .forEach(entry -> {
                                JsonObject methodValues = new JsonObject();
                                methodValues.addProperty("notch", entry.getValue());
                                methods.add(entry.getKey().getMcpName() + entry.getKey().getSignature(), methodValues);
                            });
                    mcpToSrg.getMethods().entrySet().stream()
                            .filter(entry -> entry.getKey().getParentClass().equals(mcpClass))
                            .forEach(entry -> {
                                JsonObject methodValues = Optional.ofNullable(methods.get(entry.getKey().getMcpName() + entry.getKey().getSignature()))
                                        .map(JsonElement::getAsJsonObject)
                                        .orElseThrow(() -> new IllegalStateException("Missing obf field"));
                                methodValues.addProperty("searge", entry.getValue());
                                methods.add(entry.getKey().getMcpName() + entry.getKey().getSignature(), methodValues);
                            });
                }

            });

        }
        return GSON.toJson(root);
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