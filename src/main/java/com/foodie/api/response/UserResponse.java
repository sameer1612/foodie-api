package com.foodie.api.response;

import java.time.Instant;

public record UserResponse(
    Long id,
    String username,
    String email,
    String displayName,
    Instant createdAt) {

}
