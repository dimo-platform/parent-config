package com.dimo.parent.response;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.dimo.parent.exception.ErrorResolver;
import com.dimo.parent.exception.GlobalExceptionHandler;
import com.dimo.parent.security.authentication.web.SecurityResponseWriter;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

class RestResponseContractTest {
    private final RestResponseHandler handler = new RestResponseHandler();
    private final ErrorResolver resolver = new ErrorResolver(new StaticMessageSource());
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc =
                MockMvcBuilders.standaloneSetup(new ContractController(handler))
                        .setControllerAdvice(new GlobalExceptionHandler(resolver, handler))
                        .build();
    }

    @Test
    void missingEndpointKeepsNotFoundStatus() {
        var advice = new GlobalExceptionHandler(resolver, handler);
        var result =
                advice.handleMissingResource(
                        new org.springframework.web.servlet.resource.NoResourceFoundException(
                                org.springframework.http.HttpMethod.GET, "/api/v1/missing"));
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals(
                "error.resource.not.found", result.getBody().getErrors().getFirst().errorCode());
    }

    @Test
    void successHasDataAndTimestampWithoutMessageOrLegacyFields() throws Exception {
        mvc.perform(get("/contract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("item"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.pageSize").doesNotExist());
    }

    @Test
    void emptyAndNoContentKeepTheirHttpSemantics() throws Exception {
        mvc.perform(post("/contract"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.message").doesNotExist());
        mvc.perform(delete("/contract"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertNull(
                handler.handle(HttpStatus.OK, List.of("item"), "Success").getBody().getMessage());
        assertEquals(
                List.of("item"),
                handler.handle(HttpStatus.OK, List.of("item")).getBody().getData());
        // Unpaged Page instances must also be accepted by the shared handler.
        assertEquals(
                1,
                handler.handle(HttpStatus.OK, new PageImpl<>(List.of("item"))).getBody().getSize());
    }

    @Test
    void validationAndAuthorizationFailuresUseSameEnvelope() throws Exception {
        mvc.perform(put("/contract").contentType("application/json").content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(
                        jsonPath("$.errors[0].errorCode")
                                .value("error.method.argument.not.allowed"))
                .andExpect(jsonPath("$.timestamp").isString());
        mvc.perform(get("/contract/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors[0].errorCode").value("error.security.forbidden"))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    void securityFilterFailuresExposeTopLevelErrors() throws Exception {
        var mapper = new ObjectMapper().findAndRegisterModules();
        var response = new MockHttpServletResponse();
        new SecurityResponseWriter(resolver, mapper, handler)
                .write(
                        new MockHttpServletRequest("GET", "/contract"),
                        response,
                        HttpStatus.UNAUTHORIZED,
                        "error.security.unauthorized");
        var body = mapper.readTree(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertEquals("error.security.unauthorized", body.at("/errors/0/errorCode").asText());
        assertTrue(body.get("data").isObject());
        assertTrue(body.has("timestamp"));
        assertFalse(body.has("code"));
    }

    @Test
    void statusExceptionsAndOptimisticConflictsKeepTheirHttpStatus() throws Exception {
        mvc.perform(get("/contract/conflict"))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.errors[0].errorCode").value("error.concurrent.modification"));
        mvc.perform(get("/contract/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].errorCode").value("error.resource.not.found"));
        mvc.perform(get("/contract/stale")).andExpect(status().isConflict());
    }

    @RestController
    @RequestMapping("/contract")
    static class ContractController {
        private final RestResponseHandler handler;

        ContractController(RestResponseHandler handler) {
            this.handler = handler;
        }

        @GetMapping
        ResponseEntity<ResponseBody> page() {
            return handler.handle(
                    HttpStatus.OK, new PageImpl<>(List.of("item"), PageRequest.of(1, 2), 5));
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT, "Internal reason");
        }

        @GetMapping("/missing")
        void missing() {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        @GetMapping("/stale")
        void stale() {
            throw new org.springframework.dao.OptimisticLockingFailureException("Stale record");
        }

        @PostMapping
        ResponseEntity<ResponseBody> create() {
            return handler.handle(HttpStatus.CREATED, null);
        }

        @DeleteMapping
        ResponseEntity<ResponseBody> remove() {
            return handler.handle(HttpStatus.NO_CONTENT, null);
        }

        @PutMapping
        ResponseEntity<ResponseBody> validate(@Valid @RequestBody Input input) {
            return handler.handle(HttpStatus.OK, Map.of("name", input.name()));
        }

        @GetMapping("/denied")
        ResponseEntity<ResponseBody> denied() {
            throw new AccessDeniedException("denied");
        }
    }

    record Input(@NotBlank String name) {}
}
