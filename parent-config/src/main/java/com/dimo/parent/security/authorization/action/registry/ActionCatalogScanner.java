package com.dimo.parent.security.authorization.action.registry;

import com.dimo.parent.security.authorization.action.Action;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

/** Finds every {@link Action} declared on this application's controllers and their request DTOs. */
public class ActionCatalogScanner {

    private static final String PROJECT_PACKAGE = "com.dimo";
    private static final int MAXIMUM_DEPTH = 3;

    private final ApplicationContext context;


    public ActionCatalogScanner(ApplicationContext context) {
        this.context = context;
    }

    public List<ActionDescriptor> scan() {
        Map<String, ActionDescriptor> catalog = new LinkedHashMap<>();
        Set<Class<?>> visited = new HashSet<>();
        Set<Class<?>> controllers = new HashSet<>();
        for (String name : context.getBeanNamesForType(Object.class)) {
            Class<?> type = context.getType(name);
            if (type == null) {
                continue;
            }
            Class<?> userClass = ClassUtils.getUserClass(type);
            if (AnnotatedElementUtils.hasAnnotation(userClass, RestController.class)
                    || AnnotatedElementUtils.hasAnnotation(userClass, Controller.class)) {
                controllers.add(userClass);
            }
        }
        for (Class<?> controller : controllers) {
            for (Method method : controller.getDeclaredMethods()) {
                collect(method.getAnnotation(Action.class), catalog);
                for (Parameter parameter : method.getParameters()) {
                    collect(parameter.getAnnotation(Action.class), catalog);
                    collectFromType(parameter.getType(), catalog, visited, 0);
                }
            }
        }
        return new ArrayList<>(catalog.values());
    }

    private void collectFromType(
            Class<?> type, Map<String, ActionDescriptor> catalog, Set<Class<?>> visited, int depth) {
        if (depth > MAXIMUM_DEPTH || !isProjectType(type) || !visited.add(type)) {
            return;
        }
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                collect(component.getAnnotation(Action.class), catalog);
                collectFromType(component.getType(), catalog, visited, depth + 1);
            }
            return;
        }
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            collect(field.getAnnotation(Action.class), catalog);
            collectFromType(field.getType(), catalog, visited, depth + 1);
        }
    }

    private boolean isProjectType(Class<?> type) {
        return type.getPackageName().startsWith(PROJECT_PACKAGE) && !type.isInterface() && !type.isEnum()
                && !AopUtils.isAopProxy(type);
    }

    private void collect(Action action, Map<String, ActionDescriptor> catalog) {
        if (action == null) {
            return;
        }
        ActionDescriptor descriptor = new ActionDescriptor(
                action.key(), action.title(), action.titleFa(), action.group(), action.free(),
                action.denialCode().isBlank() ? null : action.denialCode(),
                action.permission().isBlank() ? null : action.permission());
        ActionDescriptor existing = catalog.putIfAbsent(descriptor.key(), descriptor);
        if (existing != null && !existing.equals(descriptor)) {
            throw new IllegalStateException("Conflicting @Action declarations for key " + descriptor.key());
        }
    }
}
