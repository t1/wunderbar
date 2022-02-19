package com.github.t1.wunderbar.junit.consumer;

import lombok.RequiredArgsConstructor;

import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

import static com.github.t1.wunderbar.junit.http.HttpUtils.readJson;

@RequiredArgsConstructor
class VariablesCollector {
    private final List<GeneratedDataPoint> generatedDataPoints;
    private final Map<String, GeneratedDataPoint> variables;
    private final String path;


    void collectBody(String body) {collect(readJson(body));}

    private void collect(JsonValue json) {
        switch (json.getValueType()) {
            case STRING:
                put(((JsonString) json).getString());
                break;
            case NUMBER:
                put(((JsonNumber) json).bigDecimalValue());
                break;
            case OBJECT:
                var jsonObject = json.asJsonObject();
                put(jsonObject);
                jsonObject.forEach((key, field) -> at(path + "/" + key).collect(field));
                break;
            case ARRAY:
                var array = json.asJsonArray();
                put(array);
                for (int i = 0; i < array.size(); i++) {
                    at(path + "/" + i).collect(array.get(i));
                }
                break;
            case NULL:
            case TRUE:
            case FALSE:
                break; // these values can't be unique
        }
    }

    private VariablesCollector at(String path) {return new VariablesCollector(generatedDataPoints, variables, path);}

    private void put(Object value) {
        GeneratedDataPoint.find(generatedDataPoints, value)
            .ifPresent(generated -> variables.put(path, generated));
    }

    void collect(Properties headers) {
        headers.stringPropertyNames().forEach(name -> collect(name, headers.getProperty(name), variables));
    }

    private void collect(String headerName, String headerValue, Map<String, GeneratedDataPoint> variables) {
        generatedDataPoints.forEach(point -> collect(point, headerName, headerValue, variables));
    }

    private void collect(GeneratedDataPoint point, String headerName, String headerValue, Map<String, GeneratedDataPoint> variables) {
        matchingPath(headerName, headerValue, point)
            .ifPresent(matchingPath -> variables.put(":" + matchingPath, point));
    }

    private Optional<String> matchingPath(String headerName, String headerValue, GeneratedDataPoint point) {
        var rawString = point.getRawValue().toString();
        var matchType = (point.getRawValue() instanceof Number) ? "(\\D)" : ".";
        var matcher = Pattern.compile("(?<prefix>" + matchType + "*?)" + rawString + "(?<suffix>" + matchType + "*)")
            .matcher(headerValue);
        if (matcher.matches()) {
            var prefix = matcher.group("prefix");
            var suffix = matcher.group("suffix");
            if (prefix.isEmpty() && suffix.isEmpty()) {
                return Optional.of(headerName);
            } else {
                return Optional.of(headerName + ":" + prefix + "{}" + suffix);
            }
        }
        return Optional.empty();
    }
}
