package com.devcommand.devcommand.integrations.identity.service;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;

public interface VerificationChallengeSender {
    void sendVerificationChallenge(String externalId, String challengeCode);
    boolean supports(ExternalIdentityProvider provider);
}
