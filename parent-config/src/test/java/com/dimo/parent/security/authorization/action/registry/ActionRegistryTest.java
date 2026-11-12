package com.dimo.parent.security.authorization.action.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dimo.parent.exception.CustomException;
import com.dimo.parent.security.authorization.action.Action;
import com.dimo.parent.security.authorization.action.ActionAuthorizer;
import com.dimo.parent.security.authorization.domain.AuthenticatedUser;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ActionRegistryTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");

    record SampleFilter(
            @Action(key = "SAMPLE_QUERY", title = "Query", titleFa = "q", group = "sample", free = true)
            String query,
            @Action(key = "SAMPLE_NEARBY", title = "Nearby", titleFa = "n", group = "sample", free = false, permission = "SAMPLE_NEARBY_READ")
            Double radius,
            @Action(key = "SAMPLE_ONLINE", title = "Online", titleFa = "o", group = "sample", free = true)
            Boolean online,
            @Action(key = "SAMPLE_TAGS", title = "Tags", titleFa = "t", group = "sample", free = true)
            Set<UUID> tags) {}

    @RestController
    static class SampleController {

        @GetMapping
        public String search(SampleFilter filter) {
            return "ok";
        }

        @GetMapping("/export")
        @Action(key = "SAMPLE_EXPORT", title = "Export", titleFa = "e", group = "sample", free = false)
        public String export() {
            return "ok";
        }

        @GetMapping("/param")
        public String param(
                @Action(key = "SAMPLE_SORT", title = "Sort", titleFa = "s", group = "sample", free = true)
                String sort) {
            return "ok";
        }
    }

    private final ActionRequirementResolver resolver = new ActionRequirementResolver();
    private final ActionAuthorizer authorizer = mock(ActionAuthorizer.class);
    private final ActionEnforcementAspect aspect = new ActionEnforcementAspect(authorizer, resolver);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void scan_collectsMethodParameterAndDtoDeclarations() {
        // Arrange
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SampleController.class);

        // Act
        List<ActionDescriptor> actions = new ActionCatalogScanner(context).scan();

        // Assert
        assertThat(actions).extracting(ActionDescriptor::key)
                .containsExactlyInAnyOrder("SAMPLE_QUERY", "SAMPLE_NEARBY", "SAMPLE_ONLINE", "SAMPLE_TAGS", "SAMPLE_EXPORT", "SAMPLE_SORT");
        assertThat(actions).filteredOn(action -> action.key().equals("SAMPLE_NEARBY")).singleElement()
                .satisfies(action -> {
                    assertThat(action.free()).isFalse();
                    assertThat(action.permission()).isEqualTo("SAMPLE_NEARBY_READ");
                    assertThat(action.group()).isEqualTo("sample");
                });
        context.close();
    }

    @Test
    void resolve_returnsOnlyKeysOfPresentFilterValues() throws Exception {
        // Arrange
        Method search = SampleController.class.getMethod("search", SampleFilter.class);
        SampleFilter filter = new SampleFilter(" ", 5.0, false, Set.of());

        // Act
        Set<String> keys = resolver.resolve(search, new Object[] {filter});

        // Assert
        assertThat(keys).containsExactly("SAMPLE_NEARBY");
        assertThat(resolver.resolve(search, new Object[] {new SampleFilter("a", null, true, Set.of(USER_ID))}))
                .containsExactlyInAnyOrder("SAMPLE_QUERY", "SAMPLE_ONLINE", "SAMPLE_TAGS");
        assertThat(resolver.resolve(search, new Object[] {new SampleFilter(null, null, null, null)})).isEmpty();
    }

    @Test
    void resolve_gatesWholeEndpointAndRequestParameters() throws Exception {
        // Act / Assert
        assertThat(resolver.resolve(SampleController.class.getMethod("export"), new Object[0])).containsExactly("SAMPLE_EXPORT");
        Method param = SampleController.class.getMethod("param", String.class);
        assertThat(resolver.resolve(param, new Object[] {"name"})).containsExactly("SAMPLE_SORT");
        assertThat(resolver.resolve(param, new Object[] {null})).isEmpty();
    }

    @Test
    void enforce_requiresEveryPresentKeyForTheCurrentUser() throws Throwable {
        // Arrange
        authenticate();
        Method search = SampleController.class.getMethod("search", SampleFilter.class);
        ProceedingJoinPoint joinPoint = joinPoint(search, new SampleFilter("a", 5.0, null, null));

        // Act
        aspect.enforce(joinPoint);

        // Assert
        verify(authorizer).require(USER_ID, "SAMPLE_QUERY");
        verify(authorizer).require(USER_ID, "SAMPLE_NEARBY");
        verify(authorizer, never()).require(eq(USER_ID), eq("SAMPLE_ONLINE"));
        verify(joinPoint).proceed();
    }

    @Test
    void enforce_doesNotProceedWhenTheAuthorizerDenies() throws Throwable {
        // Arrange
        authenticate();
        Method search = SampleController.class.getMethod("search", SampleFilter.class);
        ProceedingJoinPoint joinPoint = joinPoint(search, new SampleFilter(null, 5.0, null, null));
        Mockito.doThrow(new IllegalStateException("denied")).when(authorizer).require(USER_ID, "SAMPLE_NEARBY");

        // Act / Assert
        assertThatThrownBy(() -> aspect.enforce(joinPoint)).isInstanceOf(IllegalStateException.class);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void enforce_failsClosedWithoutAuthenticatedUserButSkipsUngatedRequests() throws Throwable {
        // Arrange
        Method search = SampleController.class.getMethod("search", SampleFilter.class);
        ProceedingJoinPoint gated = joinPoint(search, new SampleFilter("a", null, null, null));
        ProceedingJoinPoint ungated = joinPoint(search, new SampleFilter(null, null, null, null));

        // Act / Assert
        assertThatThrownBy(() -> aspect.enforce(gated)).isInstanceOf(CustomException.class);
        aspect.enforce(ungated);
        verify(ungated).proceed();
        verifyNoInteractions(authorizer);
    }

    private void authenticate() {
        AuthenticatedUser user = new AuthenticatedUser(USER_ID, "sara", List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private ProceedingJoinPoint joinPoint(Method method, Object... arguments) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(joinPoint.getTarget()).thenReturn(new SampleController());
        Mockito.when(joinPoint.getArgs()).thenReturn(arguments);
        return joinPoint;
    }

    @Test
    void verifyConfigured_failsWhenRegistryKeyIsMissingOrShort() {
        // Arrange
        com.dimo.parent.client.authorization.ActionRegistryClient client =
                mock(com.dimo.parent.client.authorization.ActionRegistryClient.class);

        // Act / Assert
        assertThatThrownBy(() -> new FeignActionRegistrar(client, "").verifyConfigured())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new FeignActionRegistrar(client, "short").verifyConfigured())
                .isInstanceOf(IllegalStateException.class);
        new FeignActionRegistrar(client, "0123456789abcdef0123456789abcdef").verifyConfigured();
        verifyNoInteractions(client);
    }
}
