package org.medcamp.feedback;

import org.medcamp.feedback.auth.AppUserDetails;
import org.medcamp.feedback.department.Department;
import org.medcamp.feedback.department.DepartmentRepository;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;
import org.medcamp.feedback.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

/** Full application with an in-memory database; each test rolls back. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class ApiTestSupport {

	protected static final String PASSWORD = "Passw0rd123";

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected UserRepository users;

	@Autowired
	protected DepartmentRepository departments;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	private int departmentCounter;

	protected Department department(String name) {
		return departments.save(new Department(name));
	}

	/** Saves a user with {@link #PASSWORD}; medical staff get their own department. */
	protected User user(String email, Role role) {
		Department department = role == Role.MEDICAL_STAFF ? department("Department " + (++departmentCounter)) : null;
		return users.save(new User(email, "Test " + role, passwordEncoder.encode(PASSWORD), role, department));
	}

	/** Runs the request as this user, with the session copy taken now. */
	protected static RequestPostProcessor as(User user) {
		return SecurityMockMvcRequestPostProcessors.user(AppUserDetails.forSession(user));
	}

}
