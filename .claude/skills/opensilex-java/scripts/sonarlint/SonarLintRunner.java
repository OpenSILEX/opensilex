import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.sonarsource.sonarlint.core.rpc.client.Sloop;
import org.sonarsource.sonarlint.core.rpc.client.SloopLauncher;
import org.sonarsource.sonarlint.core.rpc.client.SonarLintRpcClientDelegate;
import org.sonarsource.sonarlint.core.rpc.protocol.SonarLintRpcServer;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesAndTrackParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.binding.BindingConfigurationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.scope.ConfigurationScopeDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.scope.DidAddConfigurationScopesParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.file.DidUpdateFileSystemParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.ClientConstantInfoDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.HttpConfigurationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.InitializeParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.LanguageSpecificRequirements;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.OmnisharpRequirementsDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.SonarCloudAlternativeEnvironmentDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.TelemetryClientConstantAttributesDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.StandaloneRuleConfigDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.analysis.RawIssueDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.log.LogParams;
import org.sonarsource.sonarlint.core.rpc.protocol.common.ClientFileDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Language;
import org.sonarsource.sonarlint.core.rpc.protocol.common.TextRangeDto;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Headless SonarLint ("SonarQube for IDE") analysis of Java files.
 * <p>
 * Drives the official SonarLint backend (sonarlint-backend-cli, JSON-RPC) exactly like the IDE plugins do, in
 * standalone mode: no SonarQube server, no network, no telemetry. Run with
 * {@code java --class-path "<lib>/*" SonarLintRunner.java plan.json}; scripts/sonarlint/sonarlint.py builds the plan.
 * <p>
 * Output (stdout): one line per issue, {@code relative/path.java:LINE:COL [SEVERITY] java:Sxxxx message}.
 * Diagnostics go to stderr. Exit status: 0 analysis done, 2 analysis failed.
 */
public class SonarLintRunner {

    private static final class Group {
        String name;
        List<String> files = new ArrayList<>();
        List<String> libraries = new ArrayList<>();
        List<String> binaries = new ArrayList<>();
        List<String> testLibraries = new ArrayList<>();
        List<String> testBinaries = new ArrayList<>();
    }

    private static final class State {
        Path baseDir;
        boolean verbose;
        final Map<String, List<ClientFileDto>> filesByScope = new ConcurrentHashMap<>();
        final Map<String, Map<String, String>> propsByScope = new ConcurrentHashMap<>();
        final Set<String> readyScopes = ConcurrentHashMap.newKeySet();
        final List<String> logs = new CopyOnWriteArrayList<>();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: java --class-path \"<lib>/*\" SonarLintRunner.java plan.json");
            System.exit(2);
        }
        int status = 2;
        try {
            status = run(JsonParser.parseString(Files.readString(Path.of(args[0]), StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Throwable t) {
            System.err.println("SonarLintRunner: " + t);
            if (System.getenv("SONARLINT_RUNNER_DEBUG") != null) {
                t.printStackTrace();
            }
        }
        System.out.flush();
        System.exit(status);
    }

    private static int run(JsonObject plan) throws Exception {
        State st = new State();
        st.baseDir = Path.of(str(plan, "baseDir")).toAbsolutePath().normalize();
        st.verbose = plan.has("verbose") && plan.get("verbose").getAsBoolean();
        String release = str(plan, "release");
        Path jre = Path.of(str(plan, "jre"));                       // runtime of the backend (SonarLint needs Java 21+)
        Path jdkHome = Path.of(plan.has("jdkHome") ? str(plan, "jdkHome") : str(plan, "jre"));   // JDK the code is analysed against
        Path sloopDir = Path.of(str(plan, "sloopDir"));
        Path pluginsDir = Path.of(str(plan, "pluginsDir"));
        Path workDir = Path.of(str(plan, "workDir"));
        long timeoutSeconds = plan.has("timeoutSeconds") ? plan.get("timeoutSeconds").getAsLong() : 900;
        Files.createDirectories(workDir);

        List<Group> groups = new ArrayList<>();
        for (JsonElement e : plan.getAsJsonArray("groups")) {
            JsonObject g = e.getAsJsonObject();
            Group group = new Group();
            group.name = str(g, "name");
            group.files = strings(g, "files");
            group.libraries = strings(g, "libraries");
            group.binaries = strings(g, "binaries");
            group.testLibraries = strings(g, "testLibraries");
            group.testBinaries = strings(g, "testBinaries");
            groups.add(group);
        }

        // Only the Java analyzers are loaded: every other language plugin is simply not listed.
        Set<Path> plugins;
        try (Stream<Path> s = Files.list(pluginsDir)) {
            plugins = s.filter(p -> p.getFileName().toString().matches("sonar-java(-symbolic-execution)?-plugin-.*\\.jar"))
                    .collect(Collectors.toCollection(HashSet::new));
        }
        if (plugins.isEmpty()) {
            throw new IllegalStateException("no sonar-java plugin jar in " + pluginsDir);
        }

        SonarLintRpcClientDelegate delegate = delegate(st);
        if (st.verbose) {
            System.err.println("[runner] starting the SonarLint backend with " + jre + " from " + sloopDir);
        }
        Sloop sloop = new SloopLauncher(delegate).start(sloopDir, jre);
        if (st.verbose) {
            System.err.println("[runner] backend pid " + sloop.getPid() + ", alive=" + sloop.isAlive());
        }
        List<RawIssueDto> issues = new ArrayList<>();
        try {
            SonarLintRpcServer server = sloop.getRpcServer();
            Map<String, StandaloneRuleConfigDto> rules = ruleConfig(plan);
            InitializeParams init = new InitializeParams(
                    new ClientConstantInfoDto("OpenSILEX SonarLint runner", "opensilex-sonarlint-runner/1.0"),
                    new TelemetryClientConstantAttributesDto("opensilex", "OpenSILEX SonarLint runner", "1.0", "n/a", Map.of()),
                    HttpConfigurationDto.defaultConfig(),
                    new SonarCloudAlternativeEnvironmentDto(Map.of()),
                    Set.of(),                                // no backend capability: no telemetry, no synchronisation
                    workDir.resolve("storage"),
                    workDir.resolve("work"),
                    plugins,
                    Map.of(),
                    Set.of(Language.JAVA),
                    Set.of(),
                    Set.of(),
                    List.of(),
                    List.of(),
                    workDir.resolve("home").toString(),
                    rules,
                    false,
                    new LanguageSpecificRequirements(null, (OmnisharpRequirementsDto) null),
                    false,
                    null);
            server.initialize(init).get(timeoutSeconds, TimeUnit.SECONDS);

            List<ConfigurationScopeDto> scopes = new ArrayList<>();
            for (Group g : groups) {
                List<ClientFileDto> clientFiles = new ArrayList<>();
                for (String f : g.files) {
                    Path abs = st.baseDir.resolve(f).normalize();
                    boolean isTest = f.replace('\\', '/').contains("/src/test/");
                    // isUserDefined=true: the backend ignores files the "user" did not explicitly select
                    clientFiles.add(new ClientFileDto(abs.toUri(), st.baseDir.relativize(abs), g.name, isTest, "UTF-8", abs, null, Language.JAVA, true));
                }
                st.filesByScope.put(g.name, clientFiles);
                Map<String, String> props = new LinkedHashMap<>();
                props.put("sonar.java.source", release);
                props.put("sonar.java.jdkHome", jdkHome.toString());
                props.put("sonar.sourceEncoding", "UTF-8");
                putList(props, "sonar.java.libraries", g.libraries);
                putList(props, "sonar.java.binaries", g.binaries);
                putList(props, "sonar.java.test.libraries", g.testLibraries.isEmpty() ? g.libraries : g.testLibraries);
                putList(props, "sonar.java.test.binaries", g.testBinaries);
                st.propsByScope.put(g.name, props);
                scopes.add(new ConfigurationScopeDto(g.name, null, true, g.name, new BindingConfigurationDto(null, null, false)));
            }
            server.getConfigurationService().didAddConfigurationScopes(new DidAddConfigurationScopesParams(scopes));
            server.getFileService().didUpdateFileSystem(new DidUpdateFileSystemParams(
                    st.filesByScope.values().stream().flatMap(List::stream).collect(Collectors.toList()), List.of(), List.of()));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
            for (Group g : groups) {
                // standalone mode is ready within a second; 20 s is a safety net, the analysis starts anyway after it
                waitReady(st, g.name, 20);
                List<java.net.URI> uris = st.filesByScope.get(g.name).stream().map(ClientFileDto::getUri).collect(Collectors.toList());
                long remaining = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(deadline - System.nanoTime()));
                AnalyzeFilesResponse response = server.getAnalysisService()
                        .analyzeFilesAndTrack(new AnalyzeFilesAndTrackParams(g.name, UUID.randomUUID(), uris, st.propsByScope.get(g.name), false, System.currentTimeMillis()))
                        .get(remaining, TimeUnit.SECONDS);
                issues.addAll(response.getRawIssues());
                for (java.net.URI failed : response.getFailedAnalysisFiles()) {
                    System.err.println("SonarLintRunner: analysis failed for " + st.baseDir.relativize(Path.of(failed)));
                }
                if (st.verbose) {
                    System.err.println("[group " + g.name + "] " + uris.size() + " file(s), " + response.getRawIssues().size() + " issue(s)");
                }
            }
        } finally {
            try {
                // the backend stops in well under a second; after 15 s it is considered hung and abandoned
                sloop.shutdown().get(15, TimeUnit.SECONDS);
            } catch (Exception e) {
                sloop.onExit().cancel(true);
            }
        }

        issues.sort(Comparator.comparing((RawIssueDto i) -> String.valueOf(i.getFileUri()))
                .thenComparingInt(i -> i.getTextRange() == null ? 0 : i.getTextRange().getStartLine())
                .thenComparing(RawIssueDto::getRuleKey));
        for (RawIssueDto i : issues) {
            TextRangeDto r = i.getTextRange();
            String rel = i.getFileUri() == null ? "?" : st.baseDir.relativize(Path.of(i.getFileUri())).toString().replace(File.separatorChar, '/');
            String message = i.getPrimaryMessage() == null ? "" : i.getPrimaryMessage().replaceAll("\\s*[\\r\\n]+\\s*", " ");
            System.out.println(rel + ":" + (r == null ? 1 : r.getStartLine()) + ":" + (r == null ? 1 : r.getStartLineOffset() + 1)
                    + " [" + i.getSeverity() + "] " + i.getRuleKey() + " " + message);
        }
        if (st.verbose) {
            st.logs.forEach(l -> System.err.println("[backend] " + l));
        }
        return 0;
    }

    private static void waitReady(State st, String scope, int seconds) throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (!st.readyScopes.contains(scope) && System.nanoTime() < end) {
            Thread.sleep(100);
        }
    }

    private static void putList(Map<String, String> props, String key, List<String> values) {
        if (!values.isEmpty()) {
            props.put(key, String.join(",", values));
        }
    }

    private static Map<String, StandaloneRuleConfigDto> ruleConfig(JsonObject plan) throws Exception {
        Map<String, StandaloneRuleConfigDto> out = new HashMap<>();
        if (!plan.has("rulesFile") || plan.get("rulesFile").isJsonNull()) {
            return out;
        }
        Path file = Path.of(plan.get("rulesFile").getAsString());
        if (!Files.isRegularFile(file)) {
            return out;
        }
        JsonObject rules = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        Map<String, Map<String, String>> params = new HashMap<>();
        if (rules.has("parameters")) {
            for (Map.Entry<String, JsonElement> e : rules.getAsJsonObject("parameters").entrySet()) {
                Map<String, String> p = new HashMap<>();
                e.getValue().getAsJsonObject().entrySet().forEach(pe -> p.put(pe.getKey(), pe.getValue().getAsString()));
                params.put(e.getKey(), p);
            }
        }
        Set<String> disabled = new HashSet<>(strings(rules, "disabled"));
        for (String key : disabled) {
            out.put(key, new StandaloneRuleConfigDto(false, Map.of()));
        }
        for (String key : strings(rules, "enabled")) {
            out.put(key, new StandaloneRuleConfigDto(true, params.getOrDefault(key, Map.of())));
        }
        for (Map.Entry<String, Map<String, String>> e : params.entrySet()) {
            if (!disabled.contains(e.getKey())) {
                out.putIfAbsent(e.getKey(), new StandaloneRuleConfigDto(true, e.getValue()));
            }
        }
        return out;
    }

    /** Client callbacks of the backend. Everything not needed in a headless run answers with an empty value. */
    private static SonarLintRpcClientDelegate delegate(State st) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "log":
                    if (args != null && args.length == 1 && args[0] instanceof LogParams) {
                        String message = String.valueOf(((LogParams) args[0]).getMessage());
                        if (st.verbose) {
                            System.err.println("[backend] " + message);
                        } else {
                            st.logs.add(message);
                        }
                    }
                    return null;
                case "getClientLiveDescription":
                    return "OpenSILEX SonarLint runner";
                case "getBaseDir":
                    return st.baseDir;
                case "listFiles":
                    return st.filesByScope.getOrDefault(String.valueOf(args[0]), Collections.emptyList());
                case "getInferredAnalysisProperties":
                    return st.propsByScope.getOrDefault(String.valueOf(args[0]), Collections.emptyMap());
                case "getFileExclusions":
                    return Collections.emptySet();
                case "didChangeAnalysisReadiness":
                    if (Boolean.TRUE.equals(args[1])) {
                        @SuppressWarnings("unchecked")
                        Set<String> ids = (Set<String>) args[0];
                        st.readyScopes.addAll(ids);
                    }
                    return null;
                default:
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    return emptyValue(method.getReturnType());
            }
        };
        return (SonarLintRpcClientDelegate) Proxy.newProxyInstance(SonarLintRpcClientDelegate.class.getClassLoader(),
                new Class<?>[] {SonarLintRpcClientDelegate.class}, handler);
    }

    private static Object emptyValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class || type == long.class || type == short.class || type == byte.class) {
            return type == int.class ? (Object) 0 : type == long.class ? (Object) 0L : type == short.class ? (Object) (short) 0 : (Object) (byte) 0;
        }
        if (type == List.class) {
            return Collections.emptyList();
        }
        if (type == Set.class) {
            return Collections.emptySet();
        }
        if (type == Map.class) {
            return Collections.emptyMap();
        }
        if (type == Optional.class) {
            return Optional.empty();
        }
        if (type == CompletableFuture.class) {
            return CompletableFuture.completedFuture(null);
        }
        return null;
    }

    private static String str(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            throw new IllegalArgumentException("missing key in plan: " + key);
        }
        return o.get(key).getAsString();
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key) && o.get(key).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(key);
            a.forEach(e -> out.add(e.getAsString()));
        }
        return out;
    }
}
