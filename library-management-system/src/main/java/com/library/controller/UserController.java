package com.library.controller;

import com.library.exception.UserException;
import com.library.model.User;
import com.library.payload.dto.UserDTO;
import com.library.payload.response.UserStatisticsResponse;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /** Profile of the logged-in user, taken from the JWT. */
    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile() throws UserException {
        return ResponseEntity.ok(userService.getProfile());
    }

    /** All users. Admin only: a user list should never be public. */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /** Total registered users. Admin only. */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(new UserStatisticsResponse(userService.getTotalUsers()));
    }

    /** One user by id. Admins can see anyone; a normal user can only see themselves. */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) throws UserException {
        User current = userService.getCurrentUser();
        boolean isAdmin = current.getRole().name().equals("ROLE_ADMIN");
        if (!isAdmin && !current.getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }
        return ResponseEntity.ok(userService.getUserById(userId));
    }
}
