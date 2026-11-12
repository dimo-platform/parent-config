package com.dimo.parent.logging.definition;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.experimental.UtilityClass;
import com.dimo.parent.utils.JsonMapper;

import java.lang.reflect.Array;
import java.util.*;

@UtilityClass
public class DataNormalizer {

    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<>() {};


    public Object normalize(Object value) {

        if (value == null) {
            return null;
        }

        if (isSimpleValue(value)) {
            return value;
        }

        switch (value) {
            case Collection<?> collection -> {
                return normalizeCollection(collection);
            }
            case Map<?, ?> map -> {
                return normalizeMap(map);
            }
            case Object[] array -> {
                return normalizeObjectArray(array);
            }
            default -> {
            }
        }

        if (value.getClass().isArray()) {
            return normalizePrimitiveArray(value);
        }

        return normalizePojo(value);
    }


    private boolean isSimpleValue(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>
                || value instanceof UUID
                || value instanceof Character;
    }


    private Map<String, Object> normalizeMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>(mapCapacity(map.size()));

        map.forEach((key, value) ->
                result.put(
                        String.valueOf(key),
                        normalize(value)
                )
        );

        return result;
    }


    private List<Object> normalizeCollection(Collection<?> collection) {
        List<Object> result = new ArrayList<>(collection.size());

        for (Object item : collection) {
            result.add(normalize(item));
        }

        return result;
    }


    private List<Object> normalizeObjectArray(Object[] array) {
        List<Object> result = new ArrayList<>(array.length);

        for (Object item : array) {
            result.add(normalize(item));
        }

        return result;
    }


    private List<Object> normalizePrimitiveArray(Object value) {
        int length = Array.getLength(value);

        List<Object> result = new ArrayList<>(length);

        for (int i = 0; i < length; i++) {
            result.add(Array.get(value, i));
        }

        return result;
    }


    private Object normalizePojo(Object value) {
        try {
            return JsonMapper.toJson(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }


    private int mapCapacity(int expectedSize) {
        return (int) Math.ceil(expectedSize / 0.75) + 1;
    }
}