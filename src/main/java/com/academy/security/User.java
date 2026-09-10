package com.academy.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

//Clase S1
/**
 * The authenticated principal. Built from {@link com.academy.model.User} by
 * {@code IUserService.searchByUser}, it carries only what the token needs: the username, the
 * BCrypt hash used once at login, and the flat role names that become authorities.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {

    private String username;

    // Never leaves the server: the hash is compared at /login and then dropped.
    @JsonIgnore
    private String password;

    private boolean enabled;

    private List<String> roles;

    /**
     * Role names are used verbatim, with no {@code ROLE_} prefix, so {@code hasAuthority('ADMIN')}
     * matches while {@code hasRole('ADMIN')} does not.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles == null
                ? List.of()
                : roles.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
