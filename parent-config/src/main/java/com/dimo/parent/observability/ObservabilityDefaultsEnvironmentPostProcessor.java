package com.dimo.parent.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/// Shared observability defaults for every service, at the lowest property precedence, so a
/// service's own properties or the environment can still override them.
public class ObservabilityDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "dimoObservabilityDefaults";


    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new LinkedHashMap<>();

        // Latency percentiles are computed in Prometheus from histogram buckets (aggregable across instances).
        defaults.put("management.metrics.distribution.percentiles-histogram.http.server.requests", "true");
        defaults.put("management.metrics.distribution.percentiles-histogram.http.client.requests", "true");
        defaults.put("management.metrics.distribution.minimum-expected-value.http.server.requests", "5ms");
        defaults.put("management.metrics.distribution.maximum-expected-value.http.server.requests", "10s");
        defaults.put("management.metrics.distribution.minimum-expected-value.http.client.requests", "5ms");
        defaults.put("management.metrics.distribution.maximum-expected-value.http.client.requests", "10s");
        defaults.put("management.metrics.tags.application", "${spring.application.name:unknown}");
        defaults.put("management.metrics.tags.environment", "${DIMO_ENV:local}");
        defaults.put("management.prometheus.metrics.export.enabled", "true");
        defaults.put("management.observations.key-values.application", "${spring.application.name:unknown}");

        // Trace and span ids are generated in process for log correlation and W3C propagation.
        // Nothing is exported from here: spans leave the JVM only through the optional OpenTelemetry agent.
        defaults.put("management.tracing.sampling.probability", "${OTEL_TRACES_SAMPLER_ARG:0.1}");

        // JSON logs (enabled with LOGGING_STRUCTURED_FORMAT_CONSOLE=logstash) always pass through the masker.
        defaults.put("logging.structured.json.customizer",
                "com.dimo.parent.logging.masking.SensitiveDataJsonCustomizer");

        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, defaults));
    }
}
