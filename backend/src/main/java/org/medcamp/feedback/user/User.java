package org.medcamp.feedback.user;

import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.medcamp.feedback.department.Department;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false, length = 150)
	private String fullName;

	@Column(nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 30)
	private Role role;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id")
	private Department department;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(nullable = false)
	private boolean mustChangePassword;

	@Column(nullable = false)
	private int failedLoginAttempts;

	private Instant lockedUntil;

	private Instant lastLoginAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected User() {
	}

	public User(String email, String fullName, String passwordHash, Role role, Department department) {
		this.email = email;
		this.fullName = fullName;
		this.passwordHash = passwordHash;
		this.role = role;
		this.department = department;
	}

	public boolean isLocked(Instant now) {
		return lockedUntil != null && lockedUntil.isAfter(now);
	}

	/** Counts a wrong password; locks the account once {@code maxAttempts} is reached. */
	public void registerFailedLogin(Instant now, int maxAttempts, Duration lockoutDuration) {
		if (lockedUntil != null && !lockedUntil.isAfter(now)) {
			// The previous lock has expired, so start counting again.
			lockedUntil = null;
			failedLoginAttempts = 0;
		}
		failedLoginAttempts++;
		if (failedLoginAttempts >= maxAttempts) {
			lockedUntil = now.plus(lockoutDuration);
		}
	}

	public void registerSuccessfulLogin(Instant now) {
		failedLoginAttempts = 0;
		lockedUntil = null;
		lastLoginAt = now;
	}

	/** Sets a new password and clears any lockout. */
	public void changePassword(String newPasswordHash, boolean mustChangeOnNextLogin) {
		this.passwordHash = newPasswordHash;
		this.mustChangePassword = mustChangeOnNextLogin;
		this.failedLoginAttempts = 0;
		this.lockedUntil = null;
	}

	public void requirePasswordChange() {
		this.mustChangePassword = true;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public Department getDepartment() {
		return department;
	}

	public void setDepartment(Department department) {
		this.department = department;
	}

	public Long getDepartmentId() {
		return department == null ? null : department.getId();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isMustChangePassword() {
		return mustChangePassword;
	}

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
