package net.kdt.pojavlaunch.cosmetics.source;

import net.kdt.pojavlaunch.cosmetics.CsCosmeticItem;

import java.util.List;

/**
 * Pluggable catalogue source. Local is always available; remote sources optional.
 */
public interface CsCosmeticSource {
    String getId();
    String getDisplayName();
    boolean isAvailable();
    List<CsCosmeticItem> fetchPage(String query, int page, int pageSize) throws Exception;
}
