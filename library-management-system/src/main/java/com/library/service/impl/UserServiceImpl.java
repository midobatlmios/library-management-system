package com.library.service.impl;

import com.library.exception.UserException;
import com.library.mapper.UserMapper;
import com.library.model.User;
import com.library.payload.dto.UserDTO;
import com.library.repository.UserRepository;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    /**
     * The logged-in user, read from the JWT (JwtValidator puts the email in the security context).
     * Other modules (loans, reviews, wishlist...) will reuse this.
     */
    @Override
    public User getCurrentUser() throws UserException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new UserException("User is not authenticated");
        }
        User user = userRepository.findByEmail(auth.getName());
        if (user == null) {
            throw new UserException("User not found with email: " + auth.getName());
        }
        return user;
    }

    @Override
    public UserDTO getProfile() throws UserException {
        return UserMapper.toDTO(getCurrentUser());
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return UserMapper.toDTOList(userRepository.findAll());
    }

    @Override
    public UserDTO getUserById(Long id) throws UserException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found with id: " + id));
        return UserMapper.toDTO(user);
    }

    @Override
    public long getTotalUsers() {
        return userRepository.count();
    }
}
