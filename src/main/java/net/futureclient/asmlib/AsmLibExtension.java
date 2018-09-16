package net.futureclient.asmlib;

import com.google.gson.*;
import net.futureclient.asmlib.forgegradle.ForgeGradleVersion;
import net.futureclient.asmlib.parser.srg.BasicClassInfoMap;
import net.futureclient.asmlib.parser.srg.SrgMap;
import net.futureclient.asmlib.parser.srg.SrgParser;
import net.futureclient.asmlib.parser.srg.member.FieldMember;
import net.futureclient.asmlib.parser.srg.member.MethodMember;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AsmLibExtension {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String CLASS_TRANSFORMER = "Lnet/futureclient/asm/transformer/annotation/Transformer;";
    private final String CLASS_INJECT = "Lnet/futureclient/asm/transformer/annotation/Inject;";
    private final String MAPPING_TYPE_FILE = "asmlib.mappingtype"; // the file that will say what mapping type this project was compiled with

    private final Project project;
    private final ForgeGradleVersion forgeGradleVersion;

    // map SourceSet to mappingFile output and config files
    private final Map<SourceSet, ProjectEntry> asmLibSourceSets = new HashMap<>();
    private final Set<Path> toDeleteAfterBuild = new HashSet<>(); // files in the resources folder that we need to delete

    private File mcpToNotch;
    private File mcpToSrg;


    @Input
    public @Nullable String mappingType;

    public AsmLibExtension(Project project, ForgeGradleVersion forgeGradleVersion) {
        this.project = project;
        this.forgeGradleVersion = forgeGradleVersion;
        project.afterEvaluate(p -> {
            if (asmLibSourceSets.isEmpty())
                throw new IllegalStateException("No SourceSets have been added!");
        });
    }


    public void add(SourceSet sourceSet, String mappingOut, String[] mappingConfigs) {
        final ProjectEntry entry = new ProjectEntry(mappingOut, new HashSet<>(Arrays.asList(mappingConfigs)));
        asmLibSourceSets.put(sourceSet, entry);

        project.afterEvaluate(__ -> this.configure(sourceSet));
    }

    // overload for gradle
    public void add(SourceSet sourceSet, String mappingOut, Collection<String> mappingConfigs) {
        add(sourceSet, mappingOut, mappingConfigs.toArray(new String[0]));
    }


    @SuppressWarnings("unchecked")
    private void configure(SourceSet sourceSet) {
        Task t = project.getTasks().getByName(sourceSet.getCompileJavaTaskName());
        Objects.requireNonNull(t);
        if (!(t instanceof JavaCompile))
            throw new IllegalStateException("Can not add non-java SourceSet (" + sourceSet + ")");
        final JavaCompile compileTask = (JavaCompile) t;

        // delete files we put in the resource output so they dont stick around
        project.getTasks().getByName("assemble").doLast(__ -> {
            toDeleteAfterBuild.forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException ex) {
                    //throw new RuntimeException(ex); // something probably went wrong
                }
            });
        });

        final Path resourceOutput = this.getResourceOutput(sourceSet);
        final Path mappingOutput = resourceOutput.resolve(asmLibSourceSets.get(sourceSet).mappingFile);
        toDeleteAfterBuild.add(mappingOutput);

        configureMappingType(resourceOutput);

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
        });

        compileTask.doLast(task -> {
            final Set<String> configFiles = asmLibSourceSets.get(sourceSet).configs;
            final Set<String> transformerClasses = stream(sourceSet.getResources())
                    .filter(f -> configFiles.contains(f.getName()))
                    .map(this::getTransformerClasses)
                    .reduce(new HashSet<>(), (s1, s2) -> {
                        s1.addAll(s2);
                        return s1;
                    });

            final Path classesOutput = sourceSet.getOutput().getClassesDir().toPath();

            // these classes should be guaranteed to not have been touched by forgegradle
            final BasicClassInfoMap transformers = transformerClasses.stream()
                    .map(name -> name.replace("/", System.getProperty("file.separator")) + ".class")
                    .map(classesOutput::resolve)
                    .filter(p -> {
                        if (!Files.exists(p)) {
                            System.err.println("Unknown class file: " + p);
                            return false;
                        }
                        return true;
                    })
                    .map(this::readClassAnnotations)
                    .collect(
                            () -> new BasicClassInfoMap(new HashMap<>(), new HashMap<>(), new HashSet<>()), // TODO: make this not gay
                            (map, info) -> {
                                map.getClasses().add(info.getClassName());
                                map.getClasses().addAll(info.getReferencedClasses());

                                merge(map.getFieldMap(), info.getClassName(), info.getFields());
                                merge(map.getMethodMap(), info.getClassName(), info.getMethods());
                            },
                            (m1, m2) -> {throw new UnsupportedOperationException("parallel");} // this might be parallelizable
                    );


            final String json = serializeJson(parseSrgFile(mcpToNotch), parseSrgFile(mcpToSrg), transformers);
            try {
                Files.write(mappingOutput, json.getBytes());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void configureMappingType(Path resourceOutput) {
        final Path mappingTypeFile = resourceOutput.resolve(MAPPING_TYPE_FILE);
        toDeleteAfterBuild.add(mappingTypeFile);

        MappingType mappingType;
        if (this.mappingType != null) {
            mappingType = MappingType.valueOf(this.mappingType);
        } else {
            // if it fails to find this then its probably a forgegradle version problem
            final Set<Object> reobf = (NamedDomainObjectContainer<Object>)project.getExtensions().getByName("reobf");

            final long mappingTypesUsed = getUsedMappingTypes(reobf).count();
            if (mappingTypesUsed == 0)
                throw new IllegalStateException("Failed to find mapping type (no jar task?)");
            if (mappingTypesUsed > 1)
                throw new IllegalStateException("Ambiguous mapping type (multiple jars with different mapping types?)");

            mappingType = getUsedMappingTypes(reobf).findFirst().get();
        }


        //System.out.println("Using mapping type: " + mappingType);
        try {
            Files.write(mappingTypeFile, mappingType.name().getBytes());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Stream<MappingType> getUsedMappingTypes(Set<Object> reobf) {
        return reobf.stream()
                .map(ReobfWrapper::new)
                .map(ReobfWrapper::getMappingType)
                .distinct();
    }

    private TransformerInfo readClassAnnotations(Path path) {
        try (InputStream reader = Files.newInputStream(path)) {
            final ClassNode cn = new ClassNode();
            final ClassReader cr = new ClassReader(reader);
            cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            final String targetClass = readTypeAnnotation(cn);
            final Set<String> targetMethods = readMethodAnnotations(cn);
            final Set<String> fields = Collections.emptySet();

            return new TransformerInfo(targetClass, fields, targetMethods);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    private String readTypeAnnotation(ClassNode clazz) {
        final AnnotationNode transformer = Optional.ofNullable(clazz.visibleAnnotations)
                .flatMap(list ->
                        list.stream()
                            .filter(node -> node.desc.equals(CLASS_TRANSFORMER))
                            .findFirst()
                )
                .orElseThrow(() -> new IllegalStateException("Class \"" + clazz.name + "\" is missing @Transformer annotation"));

        return Optional.of(transformer)
                .flatMap(node -> this.<String>getAnnotationValue("target", transformer))
                .orElseGet(() ->
                    this.<Type>getAnnotationValue("value", transformer)
                            .map(Type::getInternalName)
                            .orElseThrow(() -> new IllegalStateException("@Transformer annotation in class \"" + clazz.name + "\" is missing a target"))
                );
    }

    private Stream<AnnotationNode> getInjectAnnotations(ClassNode clazz) {
        return clazz.methods.stream()
                .map(m -> m.visibleAnnotations != null ? m.visibleAnnotations : Collections.<AnnotationNode>emptyList())
                .flatMap(List::stream)
                .filter(m -> CLASS_INJECT.equals(m.desc));
    }

    private Set<String> readMethodAnnotations(ClassNode clazz) {
        return getInjectAnnotations(clazz)
                .map(node -> getTarget(node, clazz))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private String getTarget(AnnotationNode annotation, ClassNode clazz) {
        {
            final Optional<String> target = getAnnotationValue("target", annotation);
            if (target.isPresent()) return target.get();
        }

        final String methodName = (String) getAnnotationValue("name", annotation)
                .orElseThrow(() -> new IllegalStateException("@Inject is missing value \"name\" in class \"" + clazz.name + "\""));
        List<Type> args = (List<Type>) getAnnotationValue("args", annotation).orElse(Collections.emptyList());
        final String combinedArgs = args.stream()
                .map(Type::getDescriptor)
                .collect(Collectors.joining());
        final Type returnType = (Type) getAnnotationValue("ret", annotation).orElse(Type.VOID_TYPE);


        return String.format("%s(%s)%s", methodName, combinedArgs, returnType.getDescriptor());
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> getAnnotationValue(String name, AnnotationNode node) {
        Iterator<Object> iterator = node.values.iterator();
        while (iterator.hasNext()) {
            String valueName = (String) iterator.next();
            T next = (T)iterator.next();
            if (name.equals(valueName))
                return Optional.of(next);
        }
        return Optional.empty();
    }


    private Set<String> getTransformerClasses(File configFile) {
        try {
            final JsonObject root = new JsonParser().parse(Files.newBufferedReader(configFile.toPath())).getAsJsonObject();
            final JsonObject transformers = root.getAsJsonObject("transformers");
            if (transformers == null) { // TODO: use logger
                System.err.println("[AsmLib] Config file " + configFile.getName() + " is missing element \"transformers\"");
                return Collections.emptySet();
            }
            // separated by '/'
            final Set<String> fullClassNames = transformers.entrySet() // mostly copy/pasted from asmlib
                    .stream()
                    .collect(HashSet::new,
                            (set, entry) -> {
                                final String fullPackage = entry.getKey();
                                stream(entry.getValue().getAsJsonArray())
                                        .map(JsonElement::getAsString)
                                        .map(clazz -> (fullPackage + '.' + clazz).replaceAll("[./]+", "/"))
                                        .forEach(set::add);
                            },
                            Set::addAll
                    );
            return fullClassNames;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String serializeJson(SrgMap mcpToNotch, SrgMap mcpToSrg, BasicClassInfoMap toSave) {
        final JsonObject root = new JsonObject();
        {
            final JsonObject mappings = new JsonObject();
            root.add("mappings", mappings);
            // map mcp name to notch name
            final Map<String, String> classNames = mcpToNotch.getClassMap();

            classNames.forEach((mcpClass, obfClass) -> {
                if (!toSave.getClasses().contains(mcpClass)) return; // filter

                final JsonObject classJson = new JsonObject();
                mappings.add(mcpClass, classJson);

                classJson.add("notch", new JsonPrimitive(obfClass));
                {
                    final JsonObject fields = new JsonObject();
                    classJson.add("fields", fields);

                    addJsonValues(fields, mcpToNotch, mcpToSrg, mcpClass, SrgMap::getFieldMap,
                            FieldMember::getName, FieldMember::getObfName, toSave.getFieldMap().getOrDefault(mcpClass, Collections.emptySet()));
                }
                {
                    final JsonObject methods = new JsonObject();
                    classJson.add("methods", methods);

                    // getOrDefault because referenced classes return null
                    addJsonValues(methods, mcpToNotch, mcpToSrg, mcpClass, SrgMap::getMethodMap,
                            MethodMember::getCombinedName, MethodMember::getMappedName, toSave.getMethodMap().getOrDefault(mcpClass, Collections.emptySet()));
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
                                          Function<T, String> getProperty,
                                          Set<String> toSave)
    {
        final Set<T> notchMethods = getMap.apply(mcpToNotch).get(parentClass);
        final Set<T> seargeMethods = getMap.apply(mcpToSrg).get(parentClass);

        if (notchMethods == null || seargeMethods == null) return; // this class has no members of type T

        final Map<String, String> notchMap = notchMethods.stream()
                .collect(mapToSelf(getHeader, getProperty));
        final Map<String, String> seargeMap = seargeMethods.stream()
                .collect(mapToSelf(getHeader, getProperty));

        // map mcp name to a map that maps type to the type's mapping
        // (e.g allMembers.get("player").get("notch").equals("c") == true)
        // created by combining the notch and searge member maps
        final Map<String, Map<String, String>> allMembers = new LinkedHashMap<>();
        notchMap.forEach((fullMcp, notch) -> {
            if (!toSave.contains(fullMcp)) return;

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


    private static <T, K, V> Collector<T, ?, Map<K, V>> mapToSelf(Function<T, K> keyExtractor,
                                                                  Function<T, V> valueMapper)
    {
        return Collectors.toMap(
                keyExtractor,
                valueMapper,
                (k1, k2) -> {
                    throw new IllegalStateException("Duplicate key: " + k1);
                },
                LinkedHashMap::new
        );
    }

    private <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    private static <K, V> void merge(Map<K, Set<V>> map, K key, Set<V> newValues) {
        map.merge(key,
                new HashSet<>(newValues),
                (s1, s2) -> {
                    s1.addAll(s2);
                    return s1;
                });
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


    private Path getResourceOutput(SourceSet sourceSet) {
        return sourceSet.getOutput().getResourcesDir().toPath();
    }
}