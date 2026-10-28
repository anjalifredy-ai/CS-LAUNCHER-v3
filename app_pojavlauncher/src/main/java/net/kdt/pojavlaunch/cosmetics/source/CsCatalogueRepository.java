package net.kdt.pojavlaunch.cosmetics.source;

import net.kdt.pojavlaunch.cosmetics.CsCosmeticItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates multiple cosmetic sources with pagination.
 */
public class CsCatalogueRepository {

    private final Map<String, CsCosmeticSource> sources = new LinkedHashMap<>();

    public CsCatalogueRepository() {
        register(new CsLocalCatalogueSource());
        register(new CsMojangUsernameSkinSource());
        register(new CsMineSkinSource());
    }

    public void register(CsCosmeticSource source) {
        sources.put(source.getId(), source);
    }

    public List<CsCosmeticSource> listSources() {
        return new ArrayList<>(sources.values());
    }

    public List<CsCosmeticItem> fetch(String sourceId, String query, int page, int pageSize) throws Exception {
        CsCosmeticSource src = sources.get(sourceId);
        if (src == null) throw new IllegalArgumentException("Unknown source: " + sourceId);
        if (!src.isAvailable()) throw new IllegalStateException("Source unavailable: " + sourceId);
        return src.fetchPage(query, page, pageSize);
    }

    /** Merge first page from all available sources (local + remote). */
    public List<CsCosmeticItem> fetchAllFirstPage(String query, int pageSize) {
        List<CsCosmeticItem> merged = new ArrayList<>();
        for (CsCosmeticSource src : sources.values()) {
            if (!src.isAvailable()) continue;
            try {
                merged.addAll(src.fetchPage(query, 0, pageSize));
            } catch (Exception ignored) {}
        }
        return merged;
    }
}
