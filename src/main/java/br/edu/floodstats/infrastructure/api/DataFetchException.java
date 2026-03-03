package br.edu.floodstats.infrastructure.api;

public class DataFetchException extends Exception {
    public DataFetchException(String message) {
        super(message);
    }

    public DataFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
