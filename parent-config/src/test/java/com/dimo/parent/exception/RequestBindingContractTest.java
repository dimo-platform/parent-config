package com.dimo.parent.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.dimo.parent.response.RestResponseHandler;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

class RequestBindingContractTest {

    private MockMvc mvc;


    @BeforeEach
    void setUp() {
        GlobalExceptionHandler errors = new GlobalExceptionHandler(
                new ErrorResolver(new StaticMessageSource()), new RestResponseHandler());
        mvc = MockMvcBuilders.standaloneSetup(new BindingController()).setControllerAdvice(errors).build();
    }

    @Test
    void get_rejectsMissingQueryParameterAsClientError() throws Exception {
        mvc.perform(get("/binding"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].errorCode").value("error.validation"));
    }

    @Test
    void get_rejectsMissingHeaderAsClientError() throws Exception {
        mvc.perform(get("/binding").param("applicationCode", "dimo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].errorCode").value("error.validation"));
    }

    @Test
    void post_rejectsMalformedJsonWithoutExposingItsContent() throws Exception {
        mvc.perform(post("/binding").contentType(MediaType.APPLICATION_JSON).content("{\"private-value\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].errorCode").value("error.validation"))
                .andExpect(content().string(not(containsString("private-value"))));
    }

    @Test
    void get_preservesServerErrorForUnexpectedFailure() throws Exception {
        mvc.perform(get("/binding/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errors[0].errorCode").value("error.internal.server.error"));
    }

    @RestController
    static class BindingController {

        @GetMapping("/binding")
        String getValue(
                @RequestParam("applicationCode")
                String applicationCode,
                @RequestHeader("X-Required")
                String requiredHeader) {
            return applicationCode;
        }

        @PostMapping("/binding")
        String create(
                @RequestBody
                BindingBody body) {
            return body.value();
        }

        @GetMapping("/binding/failure")
        String fail() {
            throw new IllegalStateException("Internal failure");
        }
    }

    record BindingBody(String value) {}
}
