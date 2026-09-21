package com.laaguidi.SignFlow.service;

import com.laaguidi.SignFlow.model.dto.request.CreateUserRequest;
import com.laaguidi.SignFlow.model.dto.request.UpdateUserRequest;
import com.laaguidi.SignFlow.model.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {
    Page<UserResponse> getAllUsers(Pageable pageable);
    UserResponse getUserById(UUID id);
    UserResponse createUser(CreateUserRequest dto);
    UserResponse updateUser(UUID id, UpdateUserRequest dto);
    void deleteUser(UUID id);
}