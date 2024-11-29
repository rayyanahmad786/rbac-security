# Role-Based Access Control (RBAC) System with Authentication and Authorization

## Objective
This project aims to showcase the implementation of essential security concepts crucial for developing secure systems:

- **Authentication**: Ensures secure verification of user identities.
- **Authorization**: Grants access to resources based on roles.
- **Role-Based Access Control (RBAC)**: Manages user permissions by assigning roles, and controlling access to specific endpoints and actions.

## Key Features

### Authentication
- Secure user registration with encrypted passwords using `BCryptPasswordEncoder`.
- User login functionality with credential validation.
- Session management (future updates will include JWT or OAuth).

### Authorization
- Defined roles in the system:
  - **ROLE_USER**: Basic user role for creating and viewing approved posts.
  - **ROLE_ADMIN**: Admin role for approving/rejecting posts and assigning roles.
  - **ROLE_MODERATOR**: Moderator role for approving/rejecting posts.
- Endpoint access controlled based on roles using Spring Security annotations like `@PreAuthorize` and `@Secured`.

### Role-Based Access Control (RBAC)
- User access to system features is determined by assigned roles.
- Admins and moderators have the ability to approve, reject, or delete posts.
- Users can create posts, but they require admin or moderator approval before becoming visible.

## System Architecture

### User Management
- Users register with a default role (**ROLE_USER**), with admins and moderators able to assign additional roles.

### Post Management
- Users can create posts, which remain in a "PENDING" state until approved.
- Admins and moderators can approve or reject posts.
- Approved posts become visible to all users.

### Database
- **User table**: Stores user details, roles, and encrypted passwords.
- **Post table**: Stores user-created posts and their approval status (PENDING, APPROVED, REJECTED).

## API Endpoints

### User APIs

| Endpoint | HTTP Method | Description | Required Role |
|----------|-------------|-------------|---------------|
| `/user/join` | POST | Register a new user with default or specified roles. | None |
| `/user/access/{userId}/{userRole}` | GET | Assign a new role to a user. | ROLE_ADMIN or ROLE_MODERATOR |
| `/user` | GET | Retrieve all users in the system. | ROLE_ADMIN |
| `/user/test` | GET | Test access for basic users. | ROLE_USER |

### Post APIs

| Endpoint | HTTP Method | Description | Required Role |
|----------|-------------|-------------|---------------|
| `/post/create` | POST | Create a new post (status defaults to PENDING). | ROLE_USER |
| `/post/viewAll` | GET | View all approved posts. | None |
| `/post/approvePost/{postId}` | GET | Approve a specific post. | ROLE_ADMIN or ROLE_MODERATOR |
| `/post/removePost/{postId}` | GET | Reject a specific post. | ROLE_ADMIN or ROLE_MODERATOR |
| `/post/approveAll` | GET | Approve all pending posts. | ROLE_ADMIN or ROLE_MODERATOR |
| `/post/rejectAll` | GET | Reject all pending posts. | ROLE_ADMIN or ROLE_MODERATOR |

## How to Run the Project

### Prerequisites
- Java 17 or higher.
- Spring Boot.
- MySQL database.

### Setup Instructions

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/rayyanahmad786/rbac-security
   ```

2. **Set Up the Database**:
   - Create a MySQL database named `vrv_group_manager`.
   - Update the `application.properties` file with your database credentials.

3. **Run the Application**:
   ```bash
   mvn spring-boot:run
   ```

4. **Test the APIs**:
   - Use Postman or any REST client to test the available endpoints.

## Project Structure

```plaintext
src
├── main
│ ├── java
│ │ ├── com.code.controller    # Controllers for User and Post APIs
│ │ ├── com.code.common        # Common UserConstant
│ │ ├── com.code.config        # Configuration and Enable Web Security
│ │ ├── com.code.entities      # Entity classes for User and Post
│ │ ├── com.code.repositories  # Repository interfaces for database operations
│ │ ├── com.code.security      # Spring Security configuration
│ ├── resources
│ ├── application.properties   # Application configuration
├── test                        # Unit and integration tests
```

## Future Enhancements
- Implement JWT-based session management.
- Add detailed logging and exception handling.
- Enhance user experience with frontend integration (React.js or Angular).
