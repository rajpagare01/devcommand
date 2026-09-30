package com.devcommand.devcommand.integrations.identity.repository;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.integrations.identity.entity.UserExternalIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalIdentityRepository extends JpaRepository<UserExternalIdentity, Long> {
    
    Optional<UserExternalIdentity> findByProviderAndExternalId(ExternalIdentityProvider provider, String externalId);
    
    List<UserExternalIdentity> findByUserId(Long userId);
    
    boolean existsByProviderAndExternalId(ExternalIdentityProvider provider, String externalId);
    
    Optional<UserExternalIdentity> findByIdAndUserId(Long id, Long userId);
}
