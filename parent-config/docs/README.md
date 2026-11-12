# Shared backend configuration

`parent-config` is a Java 21 Maven library used by the Dimo backend services. It provides shared
security, public response envelopes, logging, validation/error handling, localization, Feign setup
and common configuration. It is not a separately deployed service.

Build from the repository root:

```sh
./mvnw verify
```

Services inherit `com.dimo:dimo-parent` and depend on `com.dimo:parent-config`, both published from
this repository (see the root README). Keep shared infrastructure here and domain business rules in
their owning services. Public response envelopes must not replace internal/Feign contracts or
streaming and bodyless responses. Runtime credentials and deployment endpoints belong to the
consuming service's configuration.
