package edu.uet.library_management.domain.service;

import edu.uet.library_management.domain.dto.RegisterRequest;
import edu.uet.library_management.domain.dto.UserDto;
import edu.uet.library_management.domain.dto.UserUpdateRequest;

import java.util.List;

public interface UserService {
    List<UserDto> getAllUsers();
    UserDto createUser(RegisterRequest request);
    UserDto updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);
}
