package com.library.service;

import com.library.exception.UserException;
import com.library.model.User;
import com.library.payload.dto.UserDTO;

import java.util.List;

public interface UserService {
    User getCurrentUser() throws UserException;
    UserDTO getProfile() throws UserException;
    List<UserDTO> getAllUsers();
    UserDTO getUserById(Long id) throws UserException;
    long getTotalUsers();
}
