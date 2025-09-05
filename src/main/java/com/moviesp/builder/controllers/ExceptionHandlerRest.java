package com.moviesp.builder.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.logging.Logger;

@RestController
public class ExceptionHandlerRest {

    Logger logger = Logger.getLogger(ExceptionHandlerRest.class.getName());

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        logger.severe("Exception: " + ex.getMessage());
        logger.severe("Cause: " + (Objects.nonNull(ex.getCause()) ? ex.getCause().toString() : "N/A"));
        return ResponseEntity.status(500).body("Internal Server Error: " + ex.getMessage());
    }

}
