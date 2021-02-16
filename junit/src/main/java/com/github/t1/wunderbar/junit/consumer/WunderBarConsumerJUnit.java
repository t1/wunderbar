package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.Bar;
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

import static com.github.t1.wunderbar.junit.consumer.Level.AUTO;
import static com.github.t1.wunderbar.junit.consumer.Level.INTEGRATION;
import static com.github.t1.wunderbar.junit.consumer.Level.SYSTEM;
import static com.github.t1.wunderbar.junit.consumer.Level.UNIT;
import static com.github.t1.wunderbar.junit.consumer.WunderBarConsumerExtension.NONE;
import static java.time.temporal.ChronoUnit.NANOS;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

@Slf4j
class WunderBarConsumerJUnit implements Extension, BeforeEachCallback, AfterEachCallback {
    private static boolean initialized = false;
    private static final Map<String, Bar> BARS = new LinkedHashMap<>();
    private static final Pattern FUNCTION = Pattern.compile("(?<prefix>.*)\\{(?<method>.*)\\(\\)}(?<suffix>.*)");

    private ExtensionContext context;
    private WunderBarConsumerExtension settings;
    private String testId;
    private Bar bar;
    private Instant start;
    private final List<Proxy> proxies = new ArrayList<>();

    @Override public void beforeEach(ExtensionContext context) {
        if (!initialized) init(context);

        this.context = context;
        this.testId = testId();
        this.settings = findWunderBarTest().getClass().getAnnotation(WunderBarConsumerExtension.class);
        log.info("==================== {} test: {}", level(), testId);

        this.bar = BARS.computeIfAbsent(settings.fileName(), this::createBar);
        if (bar != null) bar.setDirectory(testId);

        forEachField(Service.class, this::createProxy);

        forEachField(SystemUnderTest.class, this::initSut);

        start = Instant.now();
    }

    private static void init(ExtensionContext context) {
        registerShutdownHook(WunderBarConsumerJUnit::shutDown, context);
        initialized = true;
    }

    private static void registerShutdownHook(CloseableResource shutDown, ExtensionContext context) {
        context.getRoot().getStore(GLOBAL).put(WunderBarConsumerJUnit.class.getName(), shutDown);
    }

    private static void shutDown() {
        log.info("shut down");
        BARS.values().forEach(Bar::close);
        BARS.clear();
        initialized = false;
    }

    private Object findWunderBarTest() {
        return context.getRequiredTestInstances().getAllInstances().stream()
            .filter(test -> test.getClass().isAnnotationPresent(WunderBarConsumerExtension.class))
            .findFirst()
            .orElseThrow(() -> new WunderBarException("annotation not found: " + WunderBarConsumerExtension.class.getName()));
    }

    private Bar createBar(String fileName) {
        if (fileName.equals(NONE)) return null;
        var archiveComment = Path.of(System.getProperty("user.dir")).getFileName().toString();
        log.info("create bar [{}] in {}", archiveComment, fileName);
        var bar = new Bar(archiveComment);
        bar.setPath(Path.of(fileName));
        return bar;
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

    private long duration() { return Duration.between(start, Instant.now()).get(NANOS) / 1_000_000L; }

    @SneakyThrows(ReflectiveOperationException.class)
    private static Object newInstance(Field field) {
        Constructor<?> constructor = field.getType().getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    @SneakyThrows(ReflectiveOperationException.class)
    private static Object getField(Object instance, Field field) {
        field.setAccessible(true);
        return field.get(instance);
    }

    @SneakyThrows(ReflectiveOperationException.class)
    private static void setField(Object instance, Field field, Object value) {
        field.setAccessible(true);
        field.set(instance, value);
    }
}
