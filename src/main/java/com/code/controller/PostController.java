package com.code.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.code.entities.Post;
import com.code.entities.PostStatus;
import com.code.repositories.PostRepository;

@RestController
@RequestMapping("/post")
public class PostController {

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private JavaMailSender mailSender;

	@PostMapping("/create")
	public ResponseEntity<String> createPost(@RequestBody Post post, Principal principal) {
		post.setStatus(PostStatus.PENDING);
		post.setUserName(principal.getName());
		postRepository.save(post);

		String responseMessage = principal.getName()
				+ " Your post published successfully, Required ADMIN/MODERATOR Action!";

		return ResponseEntity.status(HttpStatus.CREATED).body(responseMessage);
	}

	@GetMapping("/approvePost/{postId}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MODERATOR')")
	public ResponseEntity<String> approvePost(@PathVariable int postId) {
		Optional<Post> optionalPost = postRepository.findById(postId);

		if (optionalPost.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post with ID " + postId + " not found.");
		}

		Post post = optionalPost.get();
		post.setStatus(PostStatus.APPROVED);
		postRepository.save(post);

		return ResponseEntity.status(HttpStatus.OK).body("Post Approved !!");
	}

	@GetMapping("/approveAll")
	@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MODERATOR')")
	public ResponseEntity<String> approveAll() {
		List<Post> pendingPosts = postRepository.findAll().stream()
				.filter(post -> post.getStatus().equals(PostStatus.PENDING)).toList();

		if (pendingPosts.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No pending posts to approve.");
		}

		pendingPosts.forEach(post -> {
			post.setStatus(PostStatus.APPROVED);
			postRepository.save(post);
		});

		return ResponseEntity.status(HttpStatus.OK).body("Approved all posts!");
	}

	@GetMapping("/removePost/{postId}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MODERATOR')")
	public ResponseEntity<String> removePost(@PathVariable int postId) {
		Optional<Post> optionalPost = postRepository.findById(postId);

		if (optionalPost.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post with ID " + postId + " not found.");
		}

		Post post = optionalPost.get();
		post.setStatus(PostStatus.REJECTED);
		postRepository.save(post);

		return ResponseEntity.status(HttpStatus.OK).body("Post Rejected !!");
	}

	@GetMapping("/rejectAll")
	@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MODERATOR')")
	public ResponseEntity<String> rejectAll() {
		List<Post> pendingPosts = postRepository.findAll().stream()
				.filter(post -> post.getStatus().equals(PostStatus.PENDING)).toList();

		if (pendingPosts.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No pending posts to reject.");
		}

		pendingPosts.forEach(post -> {
			post.setStatus(PostStatus.REJECTED);
			postRepository.save(post);
		});

		return ResponseEntity.status(HttpStatus.OK).body("Rejected all posts!");
	}

	@GetMapping("/viewAll")
	public ResponseEntity<List<Post>> viewAll() {
		List<Post> approvedPosts = postRepository.findAll().stream()
				.filter(post -> post.getStatus().equals(PostStatus.APPROVED)).collect(Collectors.toList());

		if (approvedPosts.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}

		return ResponseEntity.status(HttpStatus.OK).body(approvedPosts);
	}

	@PostMapping("/deletePost/{postId}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public String markPostForDeletion(@PathVariable int postId) {
		Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));

		if (post.getStatus() == PostStatus.PENDING_DELETION || post.getStatus() == PostStatus.DELETED) {
			return "This post is already marked for deletion or has been deleted.";
		}

		String deletionRequestId = UUID.randomUUID().toString();
		post.setDeletionRequestId(deletionRequestId);
		post.setStatus(PostStatus.PENDING_DELETION);
		postRepository.save(post);

		sendDeletionApprovalEmail(deletionRequestId);

		return "Post marked for deletion. Awaiting approval from super admin.";
	}

	private void sendDeletionApprovalEmail(String deletionRequestId) {
		String superAdminEmail = "superadmin@example.com";

		String subject = "Post Deletion Request Pending Approval";
		String text = "A post has been marked for deletion. Please review and approve using the request ID: "
				+ deletionRequestId + "\n\n" + "To approve, visit: " + "http://localhost:9898/approveDeletion/"
				+ deletionRequestId + "\n" + "To reject, visit: " + "http://localhost:9898/rejectDeletion/"
				+ deletionRequestId;

		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(superAdminEmail);
		message.setSubject(subject);
		message.setText(text);

		mailSender.send(message);
	}

	@PutMapping("/approveDeletion/{deletionRequestId}")
	@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
	public String approvePostDeletion(@PathVariable String deletionRequestId) {
		Post post = postRepository.findAll().stream().filter(p -> p.getDeletionRequestId().equals(deletionRequestId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Post with provided deletion request ID not found"));

		if (post.getStatus() != PostStatus.PENDING_DELETION) {
			return "This post is not pending deletion.";
		}

		post.setStatus(PostStatus.DELETED);
		postRepository.save(post);

		return "Post successfully deleted.";
	}

	@PutMapping("/rejectDeletion/{deletionRequestId}")
	@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
	public String rejectPostDeletion(@PathVariable String deletionRequestId) {
		Post post = postRepository.findAll().stream().filter(p -> p.getDeletionRequestId().equals(deletionRequestId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Post with provided deletion request ID not found"));

		if (post.getStatus() != PostStatus.PENDING_DELETION) {
			return "This post is not pending deletion.";
		}

		post.setStatus(PostStatus.APPROVED);
		postRepository.save(post);

		return "Post deletion rejected. The post has been restored.";
	}
}
