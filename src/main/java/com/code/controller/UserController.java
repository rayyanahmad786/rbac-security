package com.code.controller;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.code.common.UserConstant;
import com.code.entities.User;
import com.code.repositories.UserRepository;

@RestController
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository repository;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@PostMapping("/join")
	public ResponseEntity<String> joinGroup(@RequestBody User user) {
		if (user.getRoles() == null || user.getRoles().isEmpty()) {
			user.setRoles(UserConstant.DEFAULT_ROLE);
		}

		if (user.getUserName() == null || user.getPassword() == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username and password are required.");
		}

		String encryptedPwd = passwordEncoder.encode(user.getPassword());
		user.setPassword(encryptedPwd);

		repository.save(user);

		return ResponseEntity.status(HttpStatus.CREATED).body("Hi " + user.getUserName() + ", welcome to the group!");
	}

	@GetMapping("/access/{userId}/{userRole}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MODERATOR')")
	public ResponseEntity<String> giveAccessToUser(@PathVariable int userId, @PathVariable String userRole,
			Principal principal) {
		// Find the user by ID
		Optional<User> userOptional = repository.findById(userId);
		if (!userOptional.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with ID " + userId + " not found.");
		}

		User user = userOptional.get();

		List<String> activeRoles = getRolesByLoggedInUser(principal);

		if (!activeRoles.contains(userRole)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("You don't have permission to assign the role: " + userRole);
		}

		String newRole = user.getRoles() + "," + userRole;
		user.setRoles(newRole);

		repository.save(user);

		return ResponseEntity.status(HttpStatus.OK).body("Hi " + user.getUserName() + ", the role " + userRole
				+ " has been assigned to you by " + principal.getName());
	}

	@GetMapping
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public ResponseEntity<List<User>> loadUsers() {
		List<User> users = repository.findAll();
		return ResponseEntity.ok(users);
	}

	@GetMapping("/test")
	@PreAuthorize("hasAuthority('ROLE_USER')")
	public ResponseEntity<String> testUserAccess() {
		return ResponseEntity.ok("User can only access this!");
	}

	private List<String> getRolesByLoggedInUser(Principal principal) {
		// Ensure the user exists and has roles
		User user = getLoggedInUser(principal);
		if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
			return Collections.emptyList();
		}

		List<String> assignRoles = Arrays.asList(user.getRoles().split(","));

		if (assignRoles.contains("ROLE_ADMIN")) {
			return Arrays.asList(UserConstant.ADMIN_ACCESS);
		}

		if (assignRoles.contains("ROLE_MODERATOR")) {
			return Arrays.asList(UserConstant.MODERATOR_ACCESS);
		}

		return Collections.emptyList();
	}

	private User getLoggedInUser(Principal principal) {
		return repository.findByUserName(principal.getName()).get();
	}
}