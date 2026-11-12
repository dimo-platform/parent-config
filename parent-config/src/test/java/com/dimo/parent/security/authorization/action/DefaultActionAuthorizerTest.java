package com.dimo.parent.security.authorization.action;

import org.junit.jupiter.api.Test;
import com.dimo.parent.exception.CustomException;
import com.dimo.parent.security.authorization.AuthoritiesProvider;
import com.dimo.parent.security.authorization.AuthoritiesService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.dimo.parent.security.authorization.action.factory.AuthorizationTestFactory.*;

class DefaultActionAuthorizerTest {

    @Test
    void require_failsClosedForUnknownOrUnavailablePolicyEvenForSuperAdmin() {
        // Arrange
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AuthoritiesProvider provider = mock(AuthoritiesProvider.class);
        ActionAuthorizer authorizer = new DefaultActionAuthorizer(() -> policy(1, NOW, null),
                new AuthoritiesService(provider, clock, 5000), clock);

        // Act / Assert
        assertThatThrownBy(() -> authorizer.require(USER_ID, "not.registered"))
                .isInstanceOf(AuthorizationUnavailableException.class);
        verifyNoInteractions(provider);
    }

    @Test
    void require_preservesOptionalSuperAdminBypassAndDeniesMissingPermission() {
        // Arrange
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AuthoritiesProvider provider = mock(AuthoritiesProvider.class);
        when(provider.resolveSnapshot(USER_ID)).thenReturn(authority(1, NOW, NOW.plusSeconds(5), Set.of()));
        AuthoritiesService authorities = new AuthoritiesService(provider, clock, 5000);
        ActionAuthorizer authorizer = new DefaultActionAuthorizer(() -> policy(1, NOW, "CUSTOM_READ"), authorities, clock);

        // Act / Assert
        assertThatThrownBy(() -> authorizer.require(USER_ID, "chat.message.create")).isInstanceOf(CustomException.class);
        authorities.invalidate(USER_ID);
        when(provider.resolveSnapshot(USER_ID)).thenReturn(new AuthoritySnapshotDto(USER_ID, 2, NOW,
                NOW.plusSeconds(5), Set.of("SUPER_ADMIN"), Set.of()));
        assertThatCode(() -> authorizer.require(USER_ID, "chat.message.create")).doesNotThrowAnyException();
    }

    @Test
    void require_appliesTighteningWithinFiveSecondsAndExpiresWithoutStaleFallback() {
        // Arrange
        Clock clock = mock(Clock.class);
        AtomicReference<Instant> now = new AtomicReference<>(NOW);
        when(clock.instant()).thenAnswer(call -> now.get());
        AuthoritiesProvider provider = mock(AuthoritiesProvider.class);
        when(provider.resolveSnapshot(USER_ID)).thenAnswer(call -> authority(1, now.get(), now.get().plusSeconds(5), Set.of()));
        AtomicInteger version = new AtomicInteger(1);
        ActionAuthorizer authorizer = new DefaultActionAuthorizer(() -> policy(version.get(), now.get(),
                version.get() == 1 ? null : "NEW_REQUIRED"), new AuthoritiesService(provider, clock, 5000), clock);
        authorizer.require(USER_ID, "chat.message.create");
        version.set(2);
        now.set(NOW.plusSeconds(5));

        // Act / Assert
        assertThatThrownBy(() -> authorizer.require(USER_ID, "chat.message.create")).isInstanceOf(CustomException.class);
    }

    @Test
    void isAllowed_servesWarmDecisionsWithoutAdditionalPolicyOrAuthorityCalls() {
        // Arrange
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AuthoritiesProvider source = mock(AuthoritiesProvider.class);
        when(source.resolveSnapshot(USER_ID)).thenReturn(authority(1, NOW, NOW.plusSeconds(5), Set.of("CUSTOM_READ")));
        ActionPolicyProvider policies = mock(ActionPolicyProvider.class);
        when(policies.getPolicySnapshot()).thenReturn(policy(1, NOW, "CUSTOM_READ"));
        ActionAuthorizer authorizer = new DefaultActionAuthorizer(policies, new AuthoritiesService(source, clock, 5000), clock);
        authorizer.require(USER_ID, "chat.message.create");

        // Act
        long started = System.nanoTime();
        for (int index = 0; index < 100000; index++) {
            if (!authorizer.isAllowed(USER_ID, "chat.message.create")) {
                throw new AssertionError("Unexpected denial");
            }
        }
        long elapsed = System.nanoTime() - started;

        // Assert
        verify(source, times(1)).resolveSnapshot(USER_ID);
        verify(policies, times(1)).getPolicySnapshot();
        System.out.println("AUTHORIZATION_WARM_CHECKS count=100000 sourceAuthorityCalls=1 sourcePolicyCalls=1 elapsedNanos=" + elapsed);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void require_removesRoleOrPermissionGrantsAtTheFiveSecondLease(boolean superAdmin) {
        // Arrange
        Clock clock = mock(Clock.class);
        AtomicReference<Instant> now = new AtomicReference<>(NOW);
        when(clock.instant()).thenAnswer(call -> now.get());
        AuthoritiesProvider provider = mock(AuthoritiesProvider.class);
        when(provider.resolveSnapshot(USER_ID)).thenAnswer(call -> new AuthoritySnapshotDto(USER_ID,
                now.get().equals(NOW) ? 1 : 2, now.get(), now.get().plusSeconds(5),
                superAdmin && now.get().equals(NOW) ? Set.of("SUPER_ADMIN") : Set.of("USER"),
                !superAdmin && now.get().equals(NOW) ? Set.of("CUSTOM_READ") : Set.of()));
        ActionAuthorizer authorizer = new DefaultActionAuthorizer(() -> policy(1, now.get(), "CUSTOM_READ"),
                new AuthoritiesService(provider, clock, 5000), clock);
        authorizer.require(USER_ID, "chat.message.create");
        now.set(NOW.plusSeconds(5));

        // Act / Assert
        assertThatThrownBy(() -> authorizer.require(USER_ID, "chat.message.create")).isInstanceOf(CustomException.class);
    }
}
