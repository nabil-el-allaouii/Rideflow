package com.rideflow.demo.security;

import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RideFlowUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public RideFlowUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username.toLowerCase())
            .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        return toPrincipal(user);
    }

    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        return toPrincipal(user);
    }

    private RideFlowUserPrincipal toPrincipal(User user) {
        return new RideFlowUserPrincipal(
            user.id,
            user.email,
            user.passwordHash,
            user.fullName,
            user.role,
            user.status
        );
    }
}
