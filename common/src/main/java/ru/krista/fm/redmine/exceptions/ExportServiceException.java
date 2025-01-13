package ru.krista.fm.redmine.exceptions;

public class ExportServiceException extends Exception {
    public ExportServiceException(String error) {
        super(error);
    }
    public ExportServiceException(String error, Throwable cause) {
        super(error, cause);
    }
}
