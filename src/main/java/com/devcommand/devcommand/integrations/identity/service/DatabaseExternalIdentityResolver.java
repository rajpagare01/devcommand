package com.devcommand.devcommand.integrations.identity.service;

import com.devcommand.devcommand.command.identity.ExternalIdentity;
import com.devcommand.devcommand.command.identity.ExternalIdentityResolver;
import com.devcommand.devcommand.integrations.identity.entity.UserExternalIdentity;
import com.devcommand.devcommand.integrations.identity.repository.ExternalIdentityRepository;
import com.devcommand.devcommand.user.entity.User;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Primary
public class DatabaseExternalIdentityResolver implements ExternalIdentityResolver {

    private final ExternalIdentityRepository identityRepository;
    private final IdentityNormalizer normalizer;

    public DatabaseExternalIdentityResolver(ExternalIdentityRepository identityRepository, 
                                            IdentityNormalizer normalizer) {
        this.identityRepository = identityRepository;
        this.normalizer = normalizer;
    }

    @Override
    public Optional<User> resolve(ExternalIdentity identity) {
        String normalizedId = normalizer.normalize(identity.provider(), identity.externalId());
        
        Optional<UserExternalIdentity> record = identityRepository.findByProviderAndExternalId(
                identity.provider(), normalizedId);
                
        // Only return if verified
        return record.filter(UserExternalIdentity::isVerified)
                     .map(UserExternalIdentity::getUser);
    }
}
