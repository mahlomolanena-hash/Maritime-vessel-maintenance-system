package com.example.maintenance.service;

import com.example.maintenance.domain.Role;
import com.example.maintenance.domain.User;
import com.example.maintenance.dto.request.CreateUserRequest;
import com.example.maintenance.dto.request.UpdateUserRequest;
import com.example.maintenance.dto.response.UserResponse;
import com.example.maintenance.exception.BadRequestException;
import com.example.maintenance.exception.DuplicateResourceException;
import com.example.maintenance.exception.ResourceNotFoundException;
import com.example.maintenance.repository.UserRepository;
import com.example.maintenance.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAllByOrderByFirstNameAscLastNameAsc().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(find(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(req.password()))
                .firstName(req.firstName().trim())
                .lastName(req.lastName().trim())
                .phone(req.phone())
                .roles(new HashSet<>(req.roles()))
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req, UserPrincipal actor) {
        User user = find(id);
        boolean self = user.getId().equals(actor.getId());

        if (req.roles() != null) {
            if (req.roles().isEmpty()) {
                throw new BadRequestException("A user needs at least one role");
            }
            if (self && !req.roles().contains(Role.ADMIN)) {
                throw new BadRequestException("You cannot remove your own administrator role");
            }
            user.setRoles(new HashSet<>(req.roles()));
        }
        if (req.enabled() != null) {
            if (self && !req.enabled()) {
                throw new BadRequestException("You cannot disable your own account");
            }
            user.setEnabled(req.enabled());
        }
        return UserResponse.from(userRepository.save(user));
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }
}
