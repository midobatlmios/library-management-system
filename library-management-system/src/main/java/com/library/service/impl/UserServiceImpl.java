package com.library.service.impl;

import org.springframework.http.HttpStatus;
import com.library.exception.UserException;
import com.library.mapper.UserMapper;
import com.library.model.User;
import com.library.payload.dto.UserDTO;
import com.library.repository.UserRepository;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
        // On public endpoints a visitor without a token is "anonymous", which is not a logged-in user.
        if (auth == null || auth.getName() == null || auth instanceof AnonymousAuthenticationToken) {
            throw new UserException("User is not authenticated", HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findByEmail(auth.getName());
        if (user == null) {
            throw new UserException("User not found with email: " + auth.getName(), HttpStatus.NOT_FOUND);
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
                .orElseThrow(() -> new UserException("User not found with id: " + id, HttpStatus.NOT_FOUND));
        return UserMapper.toDTO(user);
    }

    @Override
    public long getTotalUsers() {
        return userRepository.count();
    }
}
