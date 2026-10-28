package net.kdt.pojavlaunch.cosmetics.source;

import net.kdt.pojavlaunch.cosmetics.CsCosmeticItem;
import net.kdt.pojavlaunch.cosmetics.CsLocalCosmeticCatalogue;

import java.util.ArrayList;
import java.util.List;

public class CsLocalCatalogueSource implements CsCosmeticSource {

    @Override
    public String getId() { return "local"; }

    @Override
    public String getDisplayName() { return "Local Catalogue"; }

    @Override
    public boolean isAvailable() { return true; }

    @Override
    public List<CsCosmeticItem> fetchPage(String query, int page, int pageSize) {
        List<CsCosmeticItem> all = CsLocalCosmeticCatalogue.all();
        String q = query != null ? query.trim().toLowerCase() : "";
        List<CsCosmeticItem> filtered = new ArrayList<>();
        for (CsCosmeticItem i : all) {
            if (q.isEmpty() || i.name.toLowerCase().contains(q) || i.id.contains(q)) {
                filtered.add(i);
            }
        }
        int from = Math.max(0, page * pageSize);
        if (from >= filtered.size()) return new ArrayList<>();
        int to = Math.min(filtered.size(), from + pageSize);
        return new ArrayList<>(filtered.subList(from, to));
    }
}
