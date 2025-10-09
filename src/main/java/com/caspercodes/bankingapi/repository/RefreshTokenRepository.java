package com.caspercodes.bankingapi.repository;

import com.caspercodes.bankingapi.model.RefreshToken;
import com.caspercodes.bankingapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token); //Find a token by its string value
    void deleteByUser(User user); //Delete tokens by user
    boolean existsByToken(String token); //Check if a token exists for a user
}
