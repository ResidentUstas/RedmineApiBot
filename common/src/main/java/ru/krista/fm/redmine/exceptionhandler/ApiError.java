package ru.krista.fm.redmine.exceptionhandler;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.http.HttpStatusCode;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiError {
    private HttpStatusCode statusCode;
    private String message;
    private String debugMessage;
    private String url;
    private List<String> stackTrace;
    private List<FieldValidationError> fieldValidationErrors;

    public void addValidationErrors(List<FieldError> fieldErrors) {
        fieldErrors.forEach(error -> {
            FieldValidationError subError = new FieldValidationError();
            subError.setField(error.getField());
            subError.setMessage(error.getDefaultMessage());
            subError.setRejectedValue(error.getRejectedValue());
            subError.setObject(error.getObjectName());
            this.addSubError(subError);
        });
    }

    public void addStackTrace(StackTraceElement[] el) {
        stackTrace = Arrays.stream(el)
                .filter(x -> x.getClassName().matches(".*krista.*"))
                .map(StackTraceElement::toString)
                .toList();
    }

    private void addSubError(FieldValidationError subError) {
        if (fieldValidationErrors == null) fieldValidationErrors = new ArrayList<>();
        fieldValidationErrors.add(subError);
    }

    @Override
    public String toString() {
        return "ApiError{\n" +
                "statusCode=" + statusCode + ",\n" +
                "message='" + message + "',\n" +
                "debugMessage='" + debugMessage + "',\n" +
                "stackTrace=" + stackTrace + "\n" +
                '}';
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FieldValidationError {
        private String object;
        private String field;
        private Object rejectedValue;
        private String message;
    }
}
