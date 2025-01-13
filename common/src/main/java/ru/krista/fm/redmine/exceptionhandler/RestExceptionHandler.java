package ru.krista.fm.redmine.exceptionhandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.krista.fm.redmine.exceptions.ExportServiceArgumentNullException;
import ru.krista.fm.redmine.exceptions.ExportServiceException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.ParseException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode statusCode, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(statusCode)
                .message("Неверный формат запроса JSON (HttpMessageNotReadableException).")
                .debugMessage(ex.getLocalizedMessage())
                .url(request.getContextPath())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, statusCode);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers,
                                                                   HttpStatusCode statusCode, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(statusCode)
                .message("Обработчик не найден (NoHandlerFoundException).")
                .debugMessage(ex.getLocalizedMessage())
                .url(request.getContextPath())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, statusCode);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode statusCode, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(statusCode)
                .message("Аргумент метода недействителен (MethodArgumentNotValidException).")
                .debugMessage(ex.getLocalizedMessage())
                .url(request.getContextPath())
                .build();

        apiError.addValidationErrors(ex.getBindingResult().getFieldErrors());
        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.BAD_REQUEST)
                .message(String.format("Параметр: '%s' со значением: '%s' не может быть преобразован в тип: '%s' (MethodArgumentTypeMismatchException).",
                        ex.getName(),
                        ex.getValue(),
                        ex.getRequiredType().getSimpleName()))
                .url(request.getContextPath())
                .debugMessage(ex.getMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Отношение не найдено (EntityNotFoundException).")
                .debugMessage(ex.getMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ClassNotFoundException.class)
    protected ResponseEntity<Object> handleClassNotFoundException(ClassNotFoundException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Класс не найден (ClassNotFoundException).")
                .debugMessage(ex.getLocalizedMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Неизвестный тип ошибки (Exception).")
                .debugMessage(ex.getLocalizedMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvocationTargetException.class)
    protected ResponseEntity<Object> handleInvocationTarget(InvocationTargetException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Ошибка цели вызова (InvocationTargetException).")
                .debugMessage(ex.getLocalizedMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalAccessException.class)
    protected ResponseEntity<Object> handleIllegalAccess(IllegalAccessException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Ошибка доступа к данным (IllegalAccessException).")
                .debugMessage(ex.getLocalizedMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NoSuchFieldException.class)
    protected ResponseEntity<Object> handleNoSuchField(NoSuchFieldException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Поле не найдено (NoSuchFieldException).")
                .debugMessage(ex.getLocalizedMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InstantiationException.class)
    protected ResponseEntity<Object> handleInstantiation(InstantiationException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Ошибка при создании экземпляра (InstantiationException).")
                .debugMessage(ex.getLocalizedMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SQLException.class)
    protected ResponseEntity<Object> handleSQL(SQLException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("SQL ошибка (SQLException).")
                .debugMessage(ex.getLocalizedMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ParseException.class)
    protected ResponseEntity<Object> handleParse(ParseException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Ошибка разбора (ParseException).")
                .debugMessage(ex.getLocalizedMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IOException.class)
    protected ResponseEntity<Object> handleIO(IOException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Ошибка доступа к данным (IOException).")
                .debugMessage(ex.getLocalizedMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(JsonProcessingException.class)
    protected ResponseEntity<Object> handleJsonProcessing(JsonProcessingException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Ошибка обработки JSON (JsonProcessingException).")
                .debugMessage(ex.getLocalizedMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ExportServiceException.class)
    protected ResponseEntity<Object> handleExportService(ExportServiceException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Ошибка в сервисе ExportService (ExportServiceException).")
                .debugMessage(ex.getLocalizedMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Ошибка при сохранении данных (DataIntegrityViolationException).")
                .debugMessage(ex.getLocalizedMessage())
                .build();

        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ExportServiceArgumentNullException.class)
    protected ResponseEntity<Object> handleExportServiceArgumentNull(ExportServiceArgumentNullException ex, WebRequest request) {
        var apiError = ApiError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Параметр метода равен null (ExportServiceArgumentNullException).")
                .debugMessage(ex.getLocalizedMessage())
                .build();
        apiError.addStackTrace(ex.getStackTrace());
        log.error(apiError.toString());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
