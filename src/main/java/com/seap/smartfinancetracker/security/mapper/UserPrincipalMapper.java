package com.seap.smartfinancetracker.security.mapper;

import com.seap.smartfinancetracker.security.model.UserPrincipal;
import com.seap.smartfinancetracker.user.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

public class UserPrincipalMapper {
    public static UserPrincipal toUserPrincipal(User user) {
        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_{0}" + user.getRole().name())))
                .build();
    }
}
