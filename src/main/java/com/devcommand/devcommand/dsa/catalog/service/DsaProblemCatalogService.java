package com.devcommand.devcommand.dsa.catalog.service;

import com.devcommand.devcommand.dsa.catalog.entity.DsaProblemCatalogEntry;
import com.devcommand.devcommand.dsa.catalog.repository.DsaProblemCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DsaProblemCatalogService {

    private final DsaProblemCatalogRepository repository;

    public static String normalizeTitle(String rawTitle) {
        if (rawTitle == null) {
            return "";
        }
        String clean = rawTitle.trim().toLowerCase();

        // Strip common leading/trailing filler phrases
        if (clean.startsWith("the ")) {
            clean = clean.substring(4).trim();
        }
        if (clean.endsWith(" problem")) {
            clean = clean.substring(0, clean.length() - " problem".length()).trim();
        }
        if (clean.startsWith("leetcode ")) {
            clean = clean.substring("leetcode ".length()).trim();
        }
        if (clean.endsWith(" on leetcode")) {
            clean = clean.substring(0, clean.length() - " on leetcode".length()).trim();
        }

        // Remove all non-alphanumeric characters
        return clean.replaceAll("[^a-zA-Z0-9]", "");
    }

    @Transactional(readOnly = true)
    public Optional<DsaProblemCatalogEntry> findExactMatch(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalizeTitle(title);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        // Try exact match by normalized title
        Optional<DsaProblemCatalogEntry> match = repository.findByNormalizedTitle(normalized);
        if (match.isPresent()) {
            return match;
        }

        // Try case-insensitive title match
        return repository.findByTitleIgnoreCase(title.trim());
    }

    @Transactional(readOnly = true)
    public List<DsaProblemCatalogEntry> searchSimilar(String title) {
        if (title == null || title.isBlank()) {
            return List.of();
        }
        String normalized = normalizeTitle(title);
        String query = normalized.isBlank() ? title.trim() : normalized;
        return repository.searchSimilar(query);
    }
}
