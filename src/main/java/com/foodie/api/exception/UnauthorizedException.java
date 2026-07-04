package com.foodie.api.exception;

public class UnauthorizedException extends FoodieException {
  public UnauthorizedException(String message) {
    super(message);
  }
}
