package com.coffeeshop.repository;

import com.coffeeshop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
// User Repository

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword") String keyword);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    org.springframework.data.domain.Page<User> searchUsersPaginated(@Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);

    User findTopByUserCodeStartingWithOrderByUserCodeDesc(String prefix);

    org.springframework.data.domain.Page<User> findByActive(boolean active,
            org.springframework.data.domain.Pageable pageable);
}
