package com.example.maintenance.controller;

import com.example.maintenance.dto.request.CreateUserRequest;
import com.example.maintenance.dto.request.UpdateUserRequest;
import com.example.maintenance.dto.response.UserResponse;
import com.example.maintenance.security.Access;
import com.example.maintenance.security.UserPrincipal;
import com.example.maintenance.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    /** Managers need the list to pick an assignee for a work order. */
    @GetMapping
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userService.list());
    }

    @PostMapping
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.update(id, request, principal));
    }
}
