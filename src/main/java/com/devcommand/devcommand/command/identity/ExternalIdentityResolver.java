package com.devcommand.devcommand.command.identity;

import com.devcommand.devcommand.user.entity.User;
import java.util.Optional;

public interface ExternalIdentityResolver {
    Optional<User> resolve(ExternalIdentity externalIdentity);
}
