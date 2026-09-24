package com.devcommand.devcommand.security;

import com.devcommand.devcommand.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security's view of an authenticated User. Wraps the entity rather
 * than duplicating its fields; the password hash never leaves this class
 * (Spring Security uses it only internally, during authentication).
 *
 * No role/authority model has been requested yet, so every authenticated
 * user currently gets a single ROLE_USER authority. This is a placeholder
 * that keeps SecurityConfig's method-security options open for later,
 * without inventing a roles/permissions feature ahead of time.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
