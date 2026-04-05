package com.coffeeshop.service;

import com.coffeeshop.entity.User;
import com.coffeeshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User saveUser(@org.springframework.lang.NonNull User user) {
        // In a real app, handle password encryption here if password changed
        return userRepository.save(user);
    }

    public void deleteUser(@org.springframework.lang.NonNull Long id) {
        userRepository.deleteById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserById(@org.springframework.lang.NonNull Long id) {
        return userRepository.findById(id);
    }

    public java.util.List<User> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword);
    }

    public org.springframework.data.domain.Page<User> getAllUsersPaginated(
            org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public org.springframework.data.domain.Page<User> searchUsersPaginated(String keyword,
            org.springframework.data.domain.Pageable pageable) {
        return userRepository.searchUsersPaginated(keyword, pageable);
    }

    public org.springframework.data.domain.Page<User> getUsersByStatusPaginated(boolean active,
            org.springframework.data.domain.Pageable pageable) {
        return userRepository.findByActive(active, pageable);
    }

    public String generateUserCode() {
        User lastUser = userRepository.findTopByUserCodeStartingWithOrderByUserCodeDesc("S");
        if (lastUser != null && lastUser.getUserCode() != null) {
            String code = lastUser.getUserCode();
            try {
                int id = Integer.parseInt(code.substring(1));
                return String.format("S%03d", id + 1);
            } catch (NumberFormatException e) {
                return "S001";
            }
        }
        return "S001";
    }

    // Additional methods for registration usually go here
}
