package ru.krista.fm.redmine.exceptions;

public class ExportServiceArgumentNullException extends Exception {
    public ExportServiceArgumentNullException(String error) {
        super(error);
    }
    public ExportServiceArgumentNullException(String error, Throwable cause) {
        super(error, cause);
    }
}
