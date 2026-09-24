package com.devcommand.devcommand.dsa.controller;

import com.devcommand.devcommand.dsa.dto.CreateDsaProblemRequest;
import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.dto.UpdateDsaProblemRequest;
import com.devcommand.devcommand.dsa.service.DsaProblemService;
import com.devcommand.devcommand.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Every method here does exactly two things: pull the authenticated user's
 * id off the security context (via @AuthenticationPrincipal, populated by
 * JwtAuthenticationFilter), and delegate to DsaProblemService. No business
 * logic, no direct repository access.
 */
@RestController
@RequestMapping("/api/dsa")
@RequiredArgsConstructor
public class DsaProblemController {

    private final DsaProblemService dsaProblemService;

    @PostMapping
    public ResponseEntity<DsaProblemResponse> create(
            @Valid @RequestBody CreateDsaProblemRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DsaProblemResponse created = dsaProblemService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<DsaProblemResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Page<DsaProblemResponse> result = dsaProblemService.getAll(
                principal.getId(), topic, platform, difficulty, status, page, size, sortBy, direction
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DsaProblemResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(dsaProblemService.getById(id, principal.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DsaProblemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDsaProblemRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(dsaProblemService.update(id, principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        dsaProblemService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/solve")
    public ResponseEntity<DsaProblemResponse> markSolved(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(dsaProblemService.markSolved(id, principal.getId()));
    }

    @PatchMapping("/{id}/revision")
    public ResponseEntity<DsaProblemResponse> markForRevision(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(dsaProblemService.markForRevision(id, principal.getId()));
    }
}
