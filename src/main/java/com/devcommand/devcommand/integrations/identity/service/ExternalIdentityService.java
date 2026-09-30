package com.devcommand.devcommand.integrations.identity.service;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.integrations.identity.dto.IdentityResponse;
import com.devcommand.devcommand.integrations.identity.entity.UserExternalIdentity;
import com.devcommand.devcommand.integrations.identity.repository.ExternalIdentityRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import com.devcommand.devcommand.exception.ResourceConflictException;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExternalIdentityService {

    private final ExternalIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final IdentityNormalizer normalizer;
    private final ExternalIdentityVerificationService verificationService;

    public ExternalIdentityService(ExternalIdentityRepository identityRepository, 
                                   UserRepository userRepository,
                                   IdentityNormalizer normalizer,
                                   ExternalIdentityVerificationService verificationService) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.normalizer = normalizer;
        this.verificationService = verificationService;
    }

    public IdentityResponse linkIdentity(Long userId, ExternalIdentityProvider provider, String externalId) {
        String normalizedId = normalizer.normalize(provider, externalId);
        
        if (identityRepository.existsByProviderAndExternalId(provider, normalizedId)) {
            throw new ResourceConflictException("Identity already linked to an account");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        UserExternalIdentity identity = new UserExternalIdentity(user, provider, normalizedId, false);
        
        identity = identityRepository.save(identity);
        
        verificationService.startVerification(userId, identity.getId());
        
        return mapToResponse(identity);
    }
    
    @Transactional(readOnly = true)
    public List<IdentityResponse> getIdentitiesForUser(Long userId) {
        return identityRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public void unlinkIdentity(Long userId, Long identityId) {
        UserExternalIdentity identity = identityRepository.findByIdAndUserId(identityId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity not found"));
        identityRepository.delete(identity);
    }
    
    public IdentityResponse verifyIdentity(Long userId, Long identityId, String code) {
        verificationService.verify(userId, identityId, code);
        
        UserExternalIdentity identity = identityRepository.findByIdAndUserId(identityId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity not found"));
                
        return mapToResponse(identity);
    }
    
    private IdentityResponse mapToResponse(UserExternalIdentity entity) {
        return new IdentityResponse(
                entity.getId(),
                entity.getProvider(),
                entity.getExternalId(),
                entity.isVerified(),
                entity.getCreatedAt()
        );
    }
}
