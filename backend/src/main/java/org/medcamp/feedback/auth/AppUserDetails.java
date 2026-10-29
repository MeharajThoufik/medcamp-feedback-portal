package org.medcamp.feedback.auth;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The signed-in user as stored in the HTTP session. It holds no password hash once
 * authentication finishes; {@link CurrentUserRefreshFilter} keeps it in sync with the database.
 */
public class AppUserDetails implements UserDetails, CredentialsContainer {

	private final Long id;
	private final String email;
	private final String fullName;
	private final Role role;
	private final Long departmentId;
	private final boolean enabled;
	private final boolean accountNonLocked;
	private final boolean mustChangePassword;
	private String passwordHash;

	private AppUserDetails(User user, String passwordHash, boolean accountNonLocked) {
		this.id = user.getId();
		this.email = user.getEmail();
		this.fullName = user.getFullName();
		this.role = user.getRole();
		this.departmentId = user.getDepartmentId();
		this.enabled = user.isEnabled();
		this.mustChangePassword = user.isMustChangePassword();
		this.passwordHash = passwordHash;
		this.accountNonLocked = accountNonLocked;
	}

	/** For the sign-in check, which needs the password hash and lock state. */
	static AppUserDetails forAuthentication(User user, boolean locked) {
		return new AppUserDetails(user, user.getPasswordHash(), !locked);
	}

	/** For an already signed-in session. */
	public static AppUserDetails forSession(User user) {
		return new AppUserDetails(user, null, true);
	}

	/** True when the session copy still matches the database row. */
	boolean matches(User user) {
		return Objects.equals(email, user.getEmail()) && Objects.equals(fullName, user.getFullName())
				&& role == user.getRole() && Objects.equals(departmentId, user.getDepartmentId())
				&& mustChangePassword == user.isMustChangePassword();
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getFullName() {
		return fullName;
	}

	public Role getRole() {
		return role;
	}

	public Long getDepartmentId() {
		return departmentId;
	}

	public boolean isMustChangePassword() {
		return mustChangePassword;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(role.authority()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonLocked() {
		return accountNonLocked;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void eraseCredentials() {
		passwordHash = null;
	}

}
