package com.devcommand.devcommand.integrations.identity.controller;

import com.devcommand.devcommand.integrations.identity.dto.IdentityResponse;
import com.devcommand.devcommand.integrations.identity.dto.LinkIdentityRequest;
import com.devcommand.devcommand.integrations.identity.service.ExternalIdentityService;
import com.devcommand.devcommand.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integrations/identities")
public class ExternalIdentityController {

    private final ExternalIdentityService identityService;

    public ExternalIdentityController(ExternalIdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdentityResponse linkIdentity(@AuthenticationPrincipal UserPrincipal principal, 
                                         @Valid @RequestBody LinkIdentityRequest request) {
        return identityService.linkIdentity(principal.getId(), request.provider(), request.externalId());
    }

    @GetMapping
    public List<IdentityResponse> getIdentities(@AuthenticationPrincipal UserPrincipal principal) {
        return identityService.getIdentitiesForUser(principal.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkIdentity(@AuthenticationPrincipal UserPrincipal principal, 
                               @PathVariable Long id) {
        identityService.unlinkIdentity(principal.getId(), id);
    }

    @PostMapping("/{id}/verify")
    public IdentityResponse verifyIdentity(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long id,
                                           @Valid @RequestBody com.devcommand.devcommand.integrations.identity.dto.VerifyIdentityRequest request) {
        // We delegate to a VerificationService, but to keep the controller simple we can just
        // inject it here or call a method on identityService.
        return identityService.verifyIdentity(principal.getId(), id, request.code());
    }
}
