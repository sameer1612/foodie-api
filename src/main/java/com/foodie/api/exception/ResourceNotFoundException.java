package com.foodie.api.exception;

public class ResourceNotFoundException extends FoodieException {
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
