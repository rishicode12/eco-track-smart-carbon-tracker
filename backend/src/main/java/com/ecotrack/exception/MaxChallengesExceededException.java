package com.ecotrack.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MaxChallengesExceededException extends RuntimeException {
    public MaxChallengesExceededException(String message) {
        super(message);
    }
}
