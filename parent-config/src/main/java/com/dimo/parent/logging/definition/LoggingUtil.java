package com.dimo.parent.logging.definition;

import lombok.experimental.UtilityClass;
import com.dimo.parent.exception.CustomException;
import com.dimo.parent.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

@UtilityClass
public class LoggingUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUtil.class);


    public void info(
            String service,
            String className,
            String method,
            String message,
            String userId,
            Object data) {

        log("INFO", service, className, method, message, userId, data, null);
    }


    public void debug(
            String service,
            String className,
            String method,
            String message,
            String userId,
            Object data) {

        log("DEBUG", service, className, method, message, userId, data, null);
    }


    public void warn(
            String service,
            String className,
            String method,
            String message,
            String userId,
            Object data) {

        log("WARN", service, className, method, message, userId, data, null);
    }


    public void error(
            String service,
            String className,
            String method,
            String message,
            String userId,
            Object data,
            Throwable throwable) {

        log("ERROR", service, className, method, message, userId, data, throwable);
    }


    private void log(
            String level,
            String service,
            String className,
            String method,
            String message,
            String userId,
            Object data,
            Throwable throwable) {

        try {

            LogEvent event = buildEvent(level, service, className, method, message, userId, data, throwable);

            String json = JsonMapper.toJson(event);

            writeLog(level, json);

        } catch (Exception e) {

            // Never allow logging to break business logic.
            LOGGER.error("Failed to create JSON log event", e);
        }
    }


    private LogEvent buildEvent(
            String level,
            String service,
            String className,
            String method,
            String message,
            String userId,
            Object data,
            Throwable throwable) {

        LogEvent.LogEventBuilder builder = LogEvent.builder()
                .timestamp(OffsetDateTime.now().toString())
                .level(level)
                .service(service)
                .className(className)
                .method(method)
                .thread(Thread.currentThread().getName())
                .message(message);

        if (userId != null) {
            builder.userId(userId);
        }

        if (data != null) {
            builder.data(DataNormalizer.normalize(data));
        }

        if (throwable != null) {
            builder.error(buildErrorData(throwable));
        }

        return builder.build();
    }


    private ErrorData buildErrorData(Throwable throwable) {

        if (throwable instanceof CustomException customException) {

            return ErrorData.builder()
                    .exceptionType(customException.getClass().getName())
                    .errors(DataNormalizer.normalize(customException.getItems()))
                    .build();
        }

        return ErrorData.builder()
                .exceptionType(throwable.getClass().getName())
                .errors(throwable.getMessage())
                .build();
    }


    private void writeLog(String level, String json) {

        switch (level) {
            case "DEBUG" -> LOGGER.debug(json);
            case "WARN" -> LOGGER.warn(json);
            case "ERROR" -> LOGGER.error(json);
            default -> LOGGER.info(json);
        }
    }
}