// package com.example.bankcards.service;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import
// org.springframework.security.core.userdetails.UsernameNotFoundException;

// import com.example.bankcards.entity.User;
// import com.example.bankcards.repository.UserRepository;

// public class CustomUserDetailsService implements UserDetailsService {

// @Autowired
// private UserRepository userRepo;

// @Override
// public UserDetails loadUserByUsername(String email) throws
// UsernameNotFoundException {
// User user = userRepo.findByEmail(email)
// .orElseThrow(() -> new UsernameNotFoundException("User with email = " + email
// + " not exist!"));
// return new CustomUserDetails(user);
// }
// }