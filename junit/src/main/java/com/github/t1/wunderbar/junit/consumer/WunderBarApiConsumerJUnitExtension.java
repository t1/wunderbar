package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.WunderBarException;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import static com.github.t1.wunderbar.common.Utils.getField;
import static com.github.t1.wunderbar.common.Utils.name;
import static com.github.t1.wunderbar.common.Utils.setField;
import static com.github.t1.wunderbar.junit.JunitUtils.ORDER;
import static com.github.t1.wunderbar.junit.consumer.Level.AUTO;
import static com.github.t1.wunderbar.junit.consumer.Level.INTEGRATION;
import static com.github.t1.wunderbar.junit.consumer.Level.SYSTEM;
import static com.github.t1.wunderbar.junit.consumer.Level.UNIT;
import static com.github.t1.wunderbar.junit.consumer.Service.DEFAULT_ENDPOINT;
import static com.github.t1.wunderbar.junit.consumer.Technology.GRAPHQL;
import static com.github.t1.wunderbar.junit.consumer.Technology.REST;
import static com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer.NONE;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

@Slf4j
class WunderBarApiConsumerJUnitExtension implements Extension, BeforeEachCallback, AfterEachCallback, ParameterResolver {
    static WunderBarApiConsumerJUnitExtension INSTANCE;
    private static boolean initialized = false;
    static final Map<String, BarWriter> BAR_WRITERS = new LinkedHashMap<>();
    private static final Pattern FUNCTION = Pattern.compile("(?<prefix>.*)\\{(?<match>.*)\\(\\)}(?<suffix>.*)");
    private static final Pattern PORT = Pattern.compile("(?<prefix>.*)\\{(?<match>port)}(?<suffix>.*)");
    private static final Pattern TECHNOLOGY = Pattern.compile("(?<prefix>.*)\\{(?<match>technology)}(?<suffix>.*)");

    private ExtensionContext context;
    private WunderBarApiConsumer settings;
    private String testId;
    private BarWriter bar;
    private Instant start;
    private final List<Proxy<?>> proxies = new ArrayList<>();
    private final List<SomeData> dataGenerators = new ArrayList<>();
    private final List<GeneratedData> generatedData = new ArrayList<>();

    private final SomeGenerator someGenerator = new SomeGenerator() {
        private int depth = 0;

        @Override public Object generate(Some some, Type type, AnnotatedElement location) {
            var generator = dataGenerators.stream()
                .filter(gen -> gen.canGenerate(some, type, location))
                .findFirst().orElseThrow(() -> new WunderBarException("no generator registered for " + type + " at " + location));
            var value = generate(some, type, location, generator);
            if (value == null) throw new WunderBarException("the generator generated a null value: " + generator);
            log.debug("generated {}{}", value, info(location));
            generatedData.add(new GeneratedData(some, location, value));
            return value;
        }

        private <T> T generate(Some some, Type type, AnnotatedElement location, SomeData generator) {
            if (++depth > 1000) throw new WunderBarException("it seems to be an infinite loop when generating " + type);
            try {
                return generator.some(some, type, location);
            } finally {
                depth--;
            }
        }

        private String info(AnnotatedElement location) {
            if (location == null) return "";
            return " for " + location.getClass().getSimpleName().toLowerCase(ROOT) + " " + name(location) + container(location);
        }

        private String container(AnnotatedElement location) {
            if (location instanceof Field) return " in class " + ((Field) location).getDeclaringClass().getSimpleName();
            if (location instanceof Parameter) {
                var executable = ((Parameter) location).getDeclaringExecutable();
                return " of method " + executable.getDeclaringClass().getSimpleName() + "#" + executable.getName();
            }
            return "";
        }


        @Override public AnnotatedElement location(Object value) {return find(value).location;}

        @Override public Some findSomeFor(Object value) {return find(value).some;}

        private GeneratedData find(Object value) {
            return generatedData.stream().filter(item -> item.value.equals(value)).findFirst()
                .orElseThrow(() -> new WunderBarException("this value was not generated via the WunderBar @Some annotation: " + value));
        }
    };

    @AllArgsConstructor
    private static class GeneratedData {
        private final Some some;
        private final AnnotatedElement location;
        private final Object value;

        @Override public String toString() {return value + " -> " + some + ":" + location;}
    }

    @Override public void beforeEach(ExtensionContext context) {
        INSTANCE = this;
        if (!initialized) init(context);

        SomeBasics.reset();
        this.context = context;
        this.testId = testId();
        this.settings = findSettings();
        log.info("==================== {} test: {}", level(), testId);

        this.bar = BAR_WRITERS.computeIfAbsent(settings.fileName(), this::createBar);
        if (bar != null) bar.setDirectory(testId);

        registerSomeDataGenerators();
        forEachField(Some.class, this::createSomeTestData);
        forEachField(Service.class, this::createProxy);
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
        var archiveComment = archiveComment();
        log.info("create bar [{}] in {}", archiveComment, fileName);
        var writer = BarWriter.to(fileName);
        writer.setComment(archiveComment);
        return writer;
    }

    private String archiveComment() {
        return "level: " + level();
    }

    private void registerSomeDataGenerators() {
        extensionContexts()
            .map(ExtensionContext::getElement)
            .flatMap(Optional::stream)
            .map(element -> element.getAnnotation(Register.class))
            .filter(Objects::nonNull)
            .flatMap(register -> Stream.of(register.value()))
            .map(this::newInstance)
            .forEach(dataGenerators::add);
        dataGenerators.add(new SomeBasics());
    }

    private Stream<ExtensionContext> extensionContexts() {
        return Stream.iterate(context, c -> c.getParent().isPresent(), c -> c.getParent().orElseThrow());
    }

    private void forEachField(Class<? extends Annotation> annotationType, Consumer<Field> action) {
        context.getRequiredTestInstances().getAllInstances().stream()
            .flatMap(this::allFields)
            .filter(field -> isAnnotationPresent(field, annotationType))
            .sorted(ORDER)
            .forEach(action);
    }

    private boolean isAnnotationPresent(Field field, Class<? extends Annotation> annotationType) {
        return field.isAnnotationPresent(annotationType) || // for normal annotations
               field.getAnnotatedType().isAnnotationPresent(annotationType); // for TYPE_USE annotations (like Some)
    }

    private Stream<Field> allFields(Object instance) {
        Builder<Field> builder = Stream.builder();
        for (Class<?> c = instance.getClass(); c != null; c = c.getSuperclass())
            Stream.of(c.getDeclaredFields()).forEach(builder::add);
        return builder.build();
    }

    private void createSomeTestData(Field field) {
        setField(instanceFor(field), field, someGenerator.generate(someAnnotationOn(field), field.getGenericType(), field));
    }

    private Some someAnnotationOn(Field field) {
        return field.getAnnotatedType().getAnnotation(Some.class);
    }

    private void createProxy(Field field) {
        var service = field.getAnnotation(Service.class);
        Proxy<?> proxy = createProxy(field.getType(), service);
        setField(instanceFor(field), field, proxy.getStubbingProxy());
    }

    <T> Proxy<T> createProxy(Class<T> type, Service service) {
        var technology = technology(type);
        var proxy = new Proxy<>(level(), bar, type, endpoint(service, technology), technology);
        this.proxies.add(proxy);
        return proxy;
    }

    private Technology technology(Class<?> type) {
        if (type.isAnnotationPresent(GraphQLClientApi.class)) return GRAPHQL;
        if (type.isAnnotationPresent(javax.ws.rs.Path.class)) return REST;
        throw new WunderBarException("no technology recognized on " + type);
    }

    private Level level() {
        if (settings.level() != AUTO) return settings.level();
        var testName = findWunderBarTest().getClass().getName();
        if (testName.endsWith("ST")) return SYSTEM;
        if (testName.endsWith("IT")) return INTEGRATION;
        return UNIT;
    }

    @SuppressWarnings({"deprecated", "removal"})
    private URI endpoint(Service service, Technology technology) {
        var endpoint = service.endpoint();
        if (DEFAULT_ENDPOINT.equals(endpoint)) endpoint = settings.endpoint();
        endpoint = replace(endpoint, FUNCTION, this::functionCall);
        endpoint = replace(endpoint, TECHNOLOGY, __ -> technology.path());
        endpoint = replace(endpoint, PORT, __ -> Integer.toString(service.port()));
        return URI.create(endpoint);
    }

    private String replace(String endpoint, Pattern pattern, Function<String, String> function) {
        var matcher = pattern.matcher(endpoint);
        if (matcher.matches())
            endpoint = matcher.group("prefix") + function.apply(matcher.group("match")) + matcher.group("suffix");
        return endpoint;
    }

    private String functionCall(String methodName) {
        var instance = context.getRequiredTestInstance();
        var method = new EndpointInvocation(methodName, instance);
        var result = method.invoke();
        if (result == null) throw new NullPointerException("endpoint method '" + methodName + "' returned null");
        return result.toString();
    }

    /**
     * Search super classes as well as enclosing classes.
     * Careful: a nested instance is not a subclass of the enclosing class.
     */
    private static class EndpointInvocation {
        private final String methodName;
        private Object instance;
        private Method method;

        private EndpointInvocation(String methodName, Object instance) {
            this.methodName = methodName;
            this.instance = instance;
            find(instance.getClass());
            if (method == null) throw new WunderBarException("endpoint method not found '" + methodName + "'");
        }

        private void find(Class<?> type) {
            try {
                method = type.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException e) {
                if (type.getSuperclass() != null) find(type.getSuperclass());
                if (method == null && type.isMemberClass()) {
                    instance = enclosingInstance();
                    find(type.getEnclosingClass());
                }
            }
        }

        @SneakyThrows(ReflectiveOperationException.class)
        private Object enclosingInstance() {
            var field = instance.getClass().getDeclaredField("this$0");
            field.setAccessible(true);
            return field.get(instance);
        }

        @SneakyThrows(ReflectiveOperationException.class)
        private Object invoke() {
            method.setAccessible(true);
            return method.invoke(instance);
        }
    }

    private String testId() {
        var elements = new LinkedList<String>();
        extensionContexts().forEach(c -> elements.push(c.getDisplayName().replaceAll("\\(\\)$", "")));
        return String.join("/", elements);
    }

    private Object instanceFor(Field field) {
        return context.getRequiredTestInstances().findInstance(field.getDeclaringClass()).orElseThrow();
    }

    private void initSut(Field sutField) {
        Object systemUnderTest = getOrInitField(sutField);
        injectProxiesIntoSut(systemUnderTest);
    }

    private Object getOrInitField(Field sutField) {
        var testInstance = instanceFor(sutField);
        if (getField(testInstance, sutField) == null)
            setField(testInstance, sutField, newInstance(sutField.getType()));
        return getField(testInstance, sutField);
    }

    private void injectProxiesIntoSut(Object systemUnderTest) {
        Stream.of(systemUnderTest.getClass().getDeclaredFields())
            .filter(Objects::nonNull)
            .forEach(targetField -> injectProxyIntoSut(systemUnderTest, targetField));
    }

    private void injectProxyIntoSut(Object instance, Field field) {
        proxies.stream()
            .filter(proxy -> proxy.isAssignableTo(field))
            .forEach(proxy -> setField(instance, field, proxy.getSutProxy()));
    }


    @Override public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return Level.class.equals(parameterType) ||
               SomeGenerator.class.equals(parameterType) ||
               BarWriter.class.equals(parameterType) || // this is intentionally not documented
               someAnnotation(parameterContext.getParameter()) != null;
    }

    @Override public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var parameter = parameterContext.getParameter();
        if (Level.class.equals(parameter.getType())) return level();
        if (SomeGenerator.class.equals(parameter.getType())) return someGenerator;
        if (BarWriter.class.equals(parameter.getType())) return bar;
        return someGenerator.generate(someAnnotation(parameter), parameter.getParameterizedType(), parameter);
    }

    private Some someAnnotation(Parameter parameter) {
        // parameter#getDeclaredAnnotation, etc. is not sufficient for annotations with TYPE_USE (like `@Some`)
        // see https://stackoverflow.com/questions/66241813/how-to-parse-java-annotation-in-generic-type
        var annotatedTypes = parameter.getDeclaringExecutable().getAnnotatedParameterTypes();
        return (annotatedTypes.length == 0) ? null : annotatedTypes[0].getDeclaredAnnotation(Some.class);
    }


    @Override public void afterEach(ExtensionContext context) {
        var duration = duration();

        if (WunderbarExpectationBuilder.buildingExpectation != null)
            throw new WunderBarException("unfinished stubbing of " + WunderbarExpectationBuilder.buildingExpectation);

        proxies.forEach(Proxy::done);
        proxies.clear();
        generatedData.clear();
        dataGenerators.clear();

        if (bar != null) bar.setDirectory(null);
        log.info("{} took {} ms", testId, duration);
        testId = null;
        INSTANCE = null;
    }

    private long duration() {return (start == null) ? -1 : Duration.between(start, Instant.now()).get(NANOS) / 1_000_000L;}

    @SneakyThrows(ReflectiveOperationException.class)
    private <T> T newInstance(Class<T> type) {
        Object[] args = {};
        Constructor<?> found = null;
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 0) found = constructor;
            if (constructor.getParameterCount() == 1 && SomeGenerator.class.equals(constructor.getParameterTypes()[0])) {
                found = constructor;
                args = new Object[]{someGenerator};
                break;
            }
        }
        if (found == null) throw new WunderBarException("no matching constructor found for " + type);
        found.setAccessible(true);
        return type.cast(found.newInstance(args));
    }
}
