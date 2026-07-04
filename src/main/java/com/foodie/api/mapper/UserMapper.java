package com.foodie.api.mapper;

import org.mapstruct.Mapper;

import com.foodie.api.entity.User;
import com.foodie.api.response.UserResponse;

@Mapper(componentModel = "spring")
public interface UserMapper {
  UserResponse toResponse(User user);
}
