package com.coffeeshop.controller;

import com.coffeeshop.entity.User;
import com.coffeeshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String listUsers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "activePage", defaultValue = "0") int activePage,
            @RequestParam(value = "inactivePage", defaultValue = "0") int inactivePage,
            Model model) {

        int pageSize = 10;

        if (search != null && !search.isEmpty()) {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest
                    .of(activePage, pageSize);
            org.springframework.data.domain.Page<User> userPage = userService.searchUsersPaginated(search, pageable);
            model.addAttribute("searchResults", userPage.getContent());
            model.addAttribute("userPage", userPage); // compatibility
            model.addAttribute("currentPage", activePage);
            model.addAttribute("totalPages", userPage.getTotalPages());
            model.addAttribute("totalItems", userPage.getTotalElements());
            model.addAttribute("isSearching", true);
        } else {
            org.springframework.data.domain.Pageable activeRequest = org.springframework.data.domain.PageRequest
                    .of(activePage, pageSize);
            org.springframework.data.domain.Pageable inactiveRequest = org.springframework.data.domain.PageRequest
                    .of(inactivePage, pageSize);

            org.springframework.data.domain.Page<User> activeUserPage = userService.getUsersByStatusPaginated(true,
                    activeRequest);
            org.springframework.data.domain.Page<User> inactiveUserPage = userService.getUsersByStatusPaginated(false,
                    inactiveRequest);

            System.out.println("DEBUG: Active users count: " + activeUserPage.getTotalElements());
            System.out.println("DEBUG: Active users content size: " + activeUserPage.getContent().size());
            System.out.println("DEBUG: Inactive users count: " + inactiveUserPage.getTotalElements());

            model.addAttribute("activeUsers", activeUserPage.getContent());
            model.addAttribute("activePage", activePage);
            model.addAttribute("totalActivePages", activeUserPage.getTotalPages());
            model.addAttribute("totalActiveCount", activeUserPage.getTotalElements());

            model.addAttribute("inactiveUsers", inactiveUserPage.getContent());
            model.addAttribute("inactivePage", inactivePage);
            model.addAttribute("totalInactivePages", inactiveUserPage.getTotalPages());
            model.addAttribute("totalInactiveCount", inactiveUserPage.getTotalElements());

            model.addAttribute("isSearching", false);
        }

        model.addAttribute("search", search);
        return "admin/users/index";
    }

    @PostMapping("/save")
    public String saveUser(@ModelAttribute User user) {
        // Simple password handling for demo
        if (user.getId() == null) {
            // New user
            user.setPassword(passwordEncoder.encode("123456")); // Default password
            // Auto generate code if not present
            if (user.getUserCode() == null || user.getUserCode().trim().isEmpty()) {
                user.setUserCode(userService.generateUserCode());
            }
        } else {
            // Existing user - keep password if not creating new logic yet
            // Fix lint: explicitly check ID existence although implicit
            Long userId = user.getId();
            if (userId != null) {
                User existing = userService.getUserById(userId).orElse(null);
                if (existing != null) {
                    user.setPassword(existing.getPassword());
                    // Keep existing code if not provided
                    if (user.getUserCode() == null || user.getUserCode().trim().isEmpty()) {
                        user.setUserCode(existing.getUserCode());
                    }
                }
            }
        }

        // Sanitize unique fields to avoid "Duplicate entry ''" error
        if (user.getUserCode() != null && user.getUserCode().trim().isEmpty()) {
            // This case should be covered by auto-generation now, but safety check
            user.setUserCode(null);
        }
        if (user.getEmail() != null && user.getEmail().trim().isEmpty()) {
            user.setEmail(null);
        }
        if (user.getPhone() != null && user.getPhone().trim().isEmpty()) {
            user.setPhone(null);
        }

        userService.saveUser(user);
        return "redirect:/admin/users";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@org.springframework.lang.NonNull @PathVariable Long id,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            userService.deleteUser(id);
            ra.addFlashAttribute("message", "User deleted successfully.");
        } catch (Exception e) {
            // If foreign key constraint fails, fallback to deactivation
            User user = userService.getUserById(id).orElse(null);
            if (user != null) {
                user.setActive(false);
                userService.saveUser(user);
                ra.addFlashAttribute("error",
                        "Cannot permanently delete user due to existing records. User has been DEACTIVATED instead.");
            } else {
                ra.addFlashAttribute("error", "User not found.");
            }
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/toggle/{id}")
    public String toggleUserStatus(@PathVariable Long id,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        User user = userService.getUserById(id).orElse(null);
        if (user != null) {
            user.setActive(!user.isActive());
            userService.saveUser(user);
            ra.addFlashAttribute("message", "User status updated.");
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/edit/{id}")
    @ResponseBody
    public User editUser(@org.springframework.lang.NonNull @PathVariable Long id) {
        return userService.getUserById(id).orElse(null);
    }
}
