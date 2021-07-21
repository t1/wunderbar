package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.WunderBarException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import static com.github.t1.wunderbar.junit.Utils.getField;
import static com.github.t1.wunderbar.junit.Utils.setField;
import static com.github.t1.wunderbar.junit.consumer.Level.AUTO;
import static com.github.t1.wunderbar.junit.consumer.Level.INTEGRATION;
import static com.github.t1.wunderbar.junit.consumer.Level.SYSTEM;
import static com.github.t1.wunderbar.junit.consumer.Level.UNIT;
import static com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer.NONE;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

@Slf4j
class WunderBarApiConsumerJUnitExtension implements Extension, BeforeEachCallback, AfterEachCallback {
    private static boolean initialized = false;
    static final Map<String, BarWriter> BAR_WRITERS = new LinkedHashMap<>();
    private static final Pattern FUNCTION = Pattern.compile("(?<prefix>.*)\\{(?<method>.*)\\(\\)}(?<suffix>.*)");

    private ExtensionContext context;
    private WunderBarApiConsumer settings;
    private String testId;
    private BarWriter bar;
    private Instant start;
    private final List<Proxy> proxies = new ArrayList<>();

    @Override public void beforeEach(ExtensionContext context) {
        if (!initialized) init(context);

        this.context = context;
        this.testId = testId();
        this.settings = findSettings();
        log.info("==================== {} test: {}", level(), testId);

        this.bar = BAR_WRITERS.computeIfAbsent(settings.fileName(), this::createBar);
        if (bar != null) bar.setDirectory(testId);

        forEachField(Service.class, this::createProxy);
        if (proxies.isEmpty()) throw new WunderBarException("you need at least one `@Service` field in your `@WunderBarConsumerExtension` test");

        forEachField(SystemUnderTest.class, this::initSut);

        start = Instant.now();
    }

    private static void init(ExtensionContext context) {
        registerShutdownHook(WunderBarApiConsumerJUnitExtension::shutDown, context);
        initialized = true;
    }

    private static void registerShutdownHook(CloseableResource shutDown, ExtensionContext context) {
        context.getRoot().getStore(GLOBAL).put(WunderBarApiConsumerJUnitExtension.class.getName(), shutDown);
    }

    private static void shutDown() {
        log.info("shut down");
        BAR_WRITERS.values().forEach(BarWriter::close);
        BAR_WRITERS.clear();
        initialized = false;
    }

    private WunderBarApiConsumer findSettings() {
        return findWunderBarTest().getClass().getAnnotation(WunderBarApiConsumer.class);
    }

    private Object findWunderBarTest() {
        var instances = context.getRequiredTestInstances().getAllInstances().stream()
            .filter(this::isAnnotatedAsWunderBarConsumer)
            .collect(toList());
        if (instances.isEmpty()) throw new WunderBarException("annotation not found: " + WunderBarApiConsumer.class.getName());
        return instances.get(instances.size() - 1); // the innermost / closest
    }

    private boolean isAnnotatedAsWunderBarConsumer(Object test) {
        return test.getClass().isAnnotationPresent(WunderBarApiConsumer.class);
    }

    private BarWriter createBar(String fileName) {
        if (fileName.equals(NONE)) return null;
        var archiveComment = Path.of(System.getProperty("user.dir")).getFileName().toString();
        log.info("create bar [{}] in {}", archiveComment, fileName);
        var writer = BarWriter.to(fileName);
        writer.setComment(archiveComment);
        return writer;
    }

    private void forEachField(Class<? extends Annotation> annotationType, Consumer<Field> action) {
        context.getRequiredTestInstances().getAllInstances().stream()
            .flatMap(this::allFields)
            .filter(field -> field.isAnnotationPresent(annotationType))
            .forEach(action);
    }

    private Stream<Field> allFields(Object instance) {
        Builder<Field> builder = Stream.builder();
        for (Class<?> c = instance.getClass(); c != null; c = c.getSuperclass())
            Stream.of(c.getDeclaredFields()).forEach(builder::add);
        return builder.build();
    }

    private void createProxy(Field field) {
        var proxy = new Proxy(level(), bar, field.getType(), endpoint());
        setField(instanceFor(field), field, proxy.instance);
        this.proxies.add(proxy);
    }

    private Level level() {
        if (settings.level() != AUTO) return settings.level();
        var testName = findWunderBarTest().getClass().getName();
        if (testName.endsWith("ST")) return SYSTEM;
        if (testName.endsWith("IT")) return INTEGRATION;
        return UNIT;
    }

    private String endpoint() {
        var endpoint = settings.endpoint();
        var matcher = FUNCTION.matcher(endpoint);
        if (matcher.matches())
            endpoint = matcher.group("prefix") + call(matcher.group("method")) + matcher.group("suffix");
        return endpoint;
    }

    @SneakyThrows(ReflectiveOperationException.class)
    private String call(String methodName) {
        var instance = context.getRequiredTestInstance();
        var method = instance.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(instance).toString();
    }

    private String testId() {
        var elements = new LinkedList<String>();
        for (ExtensionContext c = context; c != context.getRoot() && c.getParent().isPresent(); c = c.getParent().get())
            elements.push(c.getDisplayName().replaceAll("\\(\\)$", ""));
        return String.join("/", elements);
    }

    private Object instanceFor(Field field) {
        return context.getRequiredTestInstances().findInstance(field.getDeclaringClass()).orElseThrow();
    }

    private void initSut(Field sutField) {
        var testInstance = instanceFor(sutField);
        if (getField(testInstance, sutField) == null)
            setField(testInstance, sutField, newInstance(sutField));
        var systemUnderTest = getField(testInstance, sutField);
        injectProxiesIntoSut(systemUnderTest);
    }

    private void injectProxiesIntoSut(Object systemUnderTest) {
        Stream.of(systemUnderTest.getClass().getDeclaredFields())
            .filter(Objects::nonNull)
            .forEach(targetField -> injectProxy(systemUnderTest, targetField));
    }

    private void injectProxy(Object instance, Field field) {
        proxies.stream()
            .filter(proxy -> proxy.isAssignableTo(field))
            .forEach(proxy -> setField(instance, field, proxy.instance));
    }


    @Override public void afterEach(ExtensionContext context) {
        var duration = duration();

        if (WunderbarExpectationBuilder.buildingExpectation != null)
            throw new WunderBarException("unfinished stubbing of " + WunderbarExpectationBuilder.buildingExpectation);

        proxies.forEach(Proxy::done);
        proxies.clear();

        if (bar != null) bar.setDirectory(null);
        log.info("{} took {} ms", testId, duration);
        testId = null;
    }

    private long duration() { return (start == null) ? -1 : Duration.between(start, Instant.now()).get(NANOS) / 1_000_000L; }

    @SneakyThrows(ReflectiveOperationException.class)
    private static Object newInstance(Field field) {
        Constructor<?> constructor = field.getType().getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
