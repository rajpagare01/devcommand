package com.devcommand.devcommand.dsa.integration;

import com.devcommand.devcommand.common.entity.BaseEntity;
import com.devcommand.devcommand.dsa.platform.DsaPlatform;
import com.devcommand.devcommand.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "external_dsa_accounts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "platform"})
})
public class ExternalDsaAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DsaPlatform platform;

    @Column(nullable = false)
    private String username;

    @Column(name = "profile_url")
    private String profileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExternalDsaAccountStatus status;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExternalDsaAccount other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
