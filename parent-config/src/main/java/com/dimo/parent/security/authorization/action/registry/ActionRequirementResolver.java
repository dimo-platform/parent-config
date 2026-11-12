package com.dimo.parent.security.authorization.action.registry;

import com.dimo.parent.security.authorization.action.Action;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Determines which action keys a concrete controller invocation needs, based on present values only. */
public class ActionRequirementResolver {

    private static final String PROJECT_PACKAGE = "com.dimo";

    private final Map<Class<?>, Boolean> carriers = new ConcurrentHashMap<>();


    public Set<String> resolve(Method method, Object[] arguments) {
        Set<String> keys = new LinkedHashSet<>();
        Action onMethod = method.getAnnotation(Action.class);
        if (onMethod != null) {
            keys.add(onMethod.key());
        }
        Parameter[] parameters = method.getParameters();
        for (int index = 0; index < parameters.length && index < arguments.length; index++) {
            Object value = arguments[index];
            Action onParameter = parameters[index].getAnnotation(Action.class);
            if (onParameter != null && isPresent(value)) {
                keys.add(onParameter.key());
            }
            if (value != null && carriesActions(value.getClass())) {
                collectFromObject(value, keys);
            }
        }
        return keys;
    }

    private void collectFromObject(Object value, Set<String> keys) {
        Class<?> type = value.getClass();
        try {
            if (type.isRecord()) {
                for (RecordComponent component : type.getRecordComponents()) {
                    Object componentValue = component.getAccessor().invoke(value);
                    addIfPresent(component.getAnnotation(Action.class), componentValue, keys);
                    nest(componentValue, keys);
                }
                return;
            }
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                Object fieldValue = field.get(value);
                addIfPresent(field.getAnnotation(Action.class), fieldValue, keys);
                nest(fieldValue, keys);
            }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot inspect request for action requirements", failure);
        }
    }

    private void nest(Object value, Set<String> keys) {
        if (value != null && carriesActions(value.getClass())) {
            collectFromObject(value, keys);
        }
    }

    private void addIfPresent(Action action, Object value, Set<String> keys) {
        if (action != null && isPresent(value)) {
            keys.add(action.key());
        }
    }

    private boolean carriesActions(Class<?> type) {
        if (!type.getPackageName().startsWith(PROJECT_PACKAGE) || type.isEnum() || type.isInterface()) {
            return false;
        }
        return carriers.computeIfAbsent(type, this::declaresAction);
    }

    private boolean declaresAction(Class<?> type) {
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                if (component.isAnnotationPresent(Action.class) || carriesActionsShallow(component.getType())) {
                    return true;
                }
            }
            return false;
        }
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Action.class) || carriesActionsShallow(field.getType())) {
                return true;
            }
        }
        return false;
    }

    private boolean carriesActionsShallow(Class<?> type) {
        if (!type.getPackageName().startsWith(PROJECT_PACKAGE) || type.isEnum() || type.isInterface()) {
            return false;
        }
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Action.class)) {
                return true;
            }
        }
        return type.isRecord() && java.util.Arrays.stream(type.getRecordComponents())
                .anyMatch(component -> component.isAnnotationPresent(Action.class));
    }

    /** A value counts as supplied when non-null, non-blank, non-empty, and not an explicit false flag. */
    static boolean isPresent(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof CharSequence text) {
            return !text.toString().isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        return true;
    }
}
