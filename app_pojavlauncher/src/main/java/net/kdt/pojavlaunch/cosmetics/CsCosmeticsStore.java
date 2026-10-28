package net.kdt.pojavlaunch.cosmetics;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Local cosmetics store + wardrobe persistence.
 * No fake currency. No multiplayer sync claimed.
 * Equipped skins are synced into offline launch path via {@link CsSkinLaunchSync}.
 */
public class CsCosmeticsStore {

    private static final Gson GSON = new Gson();
    private static final String PREFS = "cs_cosmetics";

    private final SharedPreferences prefs;
    private final Context context;

    private Set<String> favorites;
    private CsEquippedLoadout loadout;
    private List<CsSavedOutfit> outfits;
    private List<String> recent;
    private List<CsCosmeticItem> importedSkins;

    public CsCosmeticsStore(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        favorites = loadStringSet("favorites");
        loadout = loadLoadout();
        outfits = loadOutfits();
        recent = loadStringList("recent");
        importedSkins = loadImported();
    }

    public List<CsCosmeticItem> catalogue() {
        List<CsCosmeticItem> all = new ArrayList<>(CsLocalCosmeticCatalogue.all());
        all.addAll(importedSkins);
        return all;
    }

    public List<CsCosmeticItem> search(String query, CsCosmeticCategory category,
                                       CsCosmeticRarity rarity, CsCosmeticCollection collection,
                                       boolean ownedOnly, boolean favoritesOnly, boolean animatedOnly) {
        String q = query != null ? query.trim().toLowerCase() : "";
        List<CsCosmeticItem> out = new ArrayList<>();
        for (CsCosmeticItem item : catalogue()) {
            if (!q.isEmpty() && !item.name.toLowerCase().contains(q) && !item.id.contains(q)) continue;
            if (category != null && item.category != category) continue;
            if (rarity != null && item.rarity != rarity) continue;
            if (collection != null && item.collection != collection) continue;
            if (animatedOnly && !item.animated) continue;
            if (favoritesOnly && !favorites.contains(item.id)) continue;
            if (ownedOnly && !item.free && !isImportedSkin(item.id)) continue;
            out.add(item);
        }
        return out;
    }

    public void toggleFavorite(String id) {
        if (!favorites.add(id)) favorites.remove(id);
        prefs.edit().putStringSet("favorites", new HashSet<>(favorites)).apply();
    }

    public Set<String> getFavorites() {
        return new HashSet<>(favorites);
    }

    public void markViewed(String id) {
        recent.remove(id);
        recent.add(0, id);
        if (recent.size() > 30) recent = new ArrayList<>(recent.subList(0, 30));
        prefs.edit().putString("recent", GSON.toJson(recent)).apply();
    }

    public CsEquippedLoadout getLoadout() {
        return loadout;
    }

    public void equip(CsCosmeticCategory category, String itemId) {
        switch (category) {
            case SKINS: loadout.skinId = itemId; break;
            case CAPES: loadout.capeId = itemId; break;
            case HATS:
            case MASKS:
            case GLASSES: loadout.hatId = itemId; break;
            case WINGS: loadout.wingsId = itemId; break;
            case BACK_ACCESSORIES: loadout.backId = itemId; break;
            default: break;
        }
        saveLoadout();
        if (itemId != null) markViewed(itemId);
        if (category == CsCosmeticCategory.SKINS) {
            CsSkinLaunchSync.syncForCurrentAccount(context);
        }
    }

    public void setEmoteSlot(int index, String emoteId) {
        if (index < 0 || index > 5) return;
        while (loadout.emoteSlots.size() < 6) loadout.emoteSlots.add(null);
        loadout.emoteSlots.set(index, emoteId);
        saveLoadout();
    }

    public CsSavedOutfit saveOutfit(String name) {
        CsSavedOutfit outfit = new CsSavedOutfit(
                UUID.randomUUID().toString(),
                name == null || name.isEmpty() ? "Outfit" : name,
                copyLoadout(loadout)
        );
        outfits.add(outfit);
        prefs.edit().putString("outfits", GSON.toJson(outfits)).apply();
        return outfit;
    }

    public void applyOutfit(String id) {
        for (CsSavedOutfit o : outfits) {
            if (o.id.equals(id)) {
                loadout = copyLoadout(o.loadout);
                saveLoadout();
                CsSkinLaunchSync.syncForCurrentAccount(context);
                return;
            }
        }
    }

    public void deleteOutfit(String id) {
        outfits.removeIf(o -> o.id.equals(id));
        prefs.edit().putString("outfits", GSON.toJson(outfits)).apply();
    }

    public List<CsSavedOutfit> getOutfits() {
        return new ArrayList<>(outfits);
    }

    public void resetLoadout() {
        loadout = new CsEquippedLoadout();
        saveLoadout();
    }

    public File importedSkinsDir() {
        File dir = new File(Tools.DIR_GAME_HOME, "cs_cosmetics/skins");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public CsCosmeticItem importSkin(File source, String displayName) throws Exception {
        CsSkinImportValidator.Result validation = CsSkinImportValidator.validate(source);
        if (!validation.ok) throw new IllegalArgumentException(validation.message);

        String id = "imported_" + UUID.randomUUID().toString().substring(0, 8);
        File dest = new File(importedSkinsDir(), id + ".png");
        copyFile(source, dest);

        CsCosmeticItem item = new CsCosmeticItem();
        item.id = id;
        item.name = displayName != null ? displayName : stripExt(source.getName());
        item.description = "Imported local skin (" + validation.width + "×" + validation.height + ")";
        item.category = CsCosmeticCategory.SKINS;
        item.rarity = CsCosmeticRarity.COMMON;
        item.collection = CsCosmeticCollection.COMMUNITY;
        item.free = true;
        item.assetPath = dest.getAbsolutePath();
        item.thumbnailPath = dest.getAbsolutePath();
        item.skinModel = validation.modelHint;

        importedSkins.add(item);
        prefs.edit().putString("imported_skins", GSON.toJson(importedSkins)).apply();
        equip(CsCosmeticCategory.SKINS, id);
        return item;
    }

    public List<CsCosmeticItem> getImportedItems() {
        return new ArrayList<>(importedSkins);
    }

    public boolean isImportedSkin(String id) {
        for (CsCosmeticItem i : importedSkins) {
            if (i.id.equals(id)) return true;
        }
        return false;
    }

    public CsCosmeticVisibility visibilityFor(CsCosmeticCategory category) {
        if (category == CsCosmeticCategory.SKINS) return CsCosmeticVisibility.LOCAL_ONLY;
        return CsCosmeticVisibility.PROTOCOL_REQUIRED;
    }

    public String visibilityNote() {
        return "Local cosmetics are visible on this device only. Other players need a compatible " +
                "cosmetics client (protocol not shipped yet). Official Microsoft skin changes need MSA login.";
    }

    /** Path of currently equipped imported/local skin PNG, or null. */
    public String getEquippedLocalSkinPath() {
        if (loadout.skinId == null) return null;
        for (CsCosmeticItem i : importedSkins) {
            if (i.id.equals(loadout.skinId) && i.assetPath != null) return i.assetPath;
        }
        return null;
    }

    private void saveLoadout() {
        prefs.edit().putString("loadout", GSON.toJson(loadout)).apply();
    }

    private CsEquippedLoadout loadLoadout() {
        String raw = prefs.getString("loadout", null);
        if (raw == null) return new CsEquippedLoadout();
        try {
            CsEquippedLoadout l = GSON.fromJson(raw, CsEquippedLoadout.class);
            return l != null ? l : new CsEquippedLoadout();
        } catch (Exception e) {
            return new CsEquippedLoadout();
        }
    }

    private List<CsSavedOutfit> loadOutfits() {
        String raw = prefs.getString("outfits", null);
        if (raw == null) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<CsSavedOutfit>>(){}.getType();
            List<CsSavedOutfit> list = GSON.fromJson(raw, t);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<CsCosmeticItem> loadImported() {
        String raw = prefs.getString("imported_skins", null);
        if (raw == null) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<CsCosmeticItem>>(){}.getType();
            List<CsCosmeticItem> list = GSON.fromJson(raw, t);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Set<String> loadStringSet(String key) {
        Set<String> s = prefs.getStringSet(key, null);
        return s != null ? new HashSet<>(s) : new HashSet<>();
    }

    private List<String> loadStringList(String key) {
        String raw = prefs.getString(key, null);
        if (raw == null) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<String>>(){}.getType();
            List<String> list = GSON.fromJson(raw, t);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static CsEquippedLoadout copyLoadout(CsEquippedLoadout src) {
        CsEquippedLoadout c = new CsEquippedLoadout();
        c.skinId = src.skinId;
        c.capeId = src.capeId;
        c.hatId = src.hatId;
        c.backId = src.backId;
        c.wingsId = src.wingsId;
        c.emoteSlots = new ArrayList<>(src.emoteSlots);
        return c;
    }

    private static void copyFile(File src, File dest) throws Exception {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }
}
