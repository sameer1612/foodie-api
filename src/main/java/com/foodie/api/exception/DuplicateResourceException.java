package com.foodie.api.exception;

public class DuplicateResourceException extends FoodieException {
  public DuplicateResourceException(String message) {
    super(message);
  }
}
