package com.devcommand.devcommand.command.identity;

import com.devcommand.devcommand.user.entity.User;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory test implementation for Phase 5.2A.
 * Does not use database yet.
 */
public class InMemoryExternalIdentityResolver implements ExternalIdentityResolver {

    private final Map<ExternalIdentity, User> registry = new ConcurrentHashMap<>();

    public void register(ExternalIdentity identity, User user) {
        registry.put(identity, user);
    }

    @Override
    public Optional<User> resolve(ExternalIdentity externalIdentity) {
        return Optional.ofNullable(registry.get(externalIdentity));
    }
}
