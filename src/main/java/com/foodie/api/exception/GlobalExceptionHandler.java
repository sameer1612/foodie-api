package com.foodie.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {


  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ExceptionResponse handleResourceNotFound(ResourceNotFoundException ex) {
    return new ExceptionResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value());
  }

  @ExceptionHandler(DuplicateResourceException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ExceptionResponse handleDuplicateResource(DuplicateResourceException ex) {
    return new ExceptionResponse(ex.getMessage(), HttpStatus.CONFLICT.value());
  }


  @ExceptionHandler(StorageException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ExceptionResponse handleStorageException(StorageException ex) {
    return new ExceptionResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
  }


  @ExceptionHandler(UnauthorizedException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ExceptionResponse handleUnauthorizedException(UnauthorizedException ex) {
    return new ExceptionResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED.value());
  }


  @ExceptionHandler(ValidationException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public ExceptionResponse handleValidationException(ValidationException ex) {
    return new ExceptionResponse(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY.value());
  }
}
