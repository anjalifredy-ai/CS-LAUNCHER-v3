package net.kdt.pojavlaunch.cosmetics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Built-in free local catalogue. Expandable via CosmeticsStore sources later.
 */
public final class CsLocalCosmeticCatalogue {

    private static final List<CsCosmeticItem> ITEMS;

    static {
        List<CsCosmeticItem> list = new ArrayList<>();
        list.add(item("skin_steve", "Steve", "Classic Steve", CsCosmeticCategory.SKINS, CsCosmeticRarity.COMMON, CsCosmeticCollection.CLASSIC_PVP));
        list.add(item("skin_alex", "Alex", "Classic Alex (slim)", CsCosmeticCategory.SKINS, CsCosmeticRarity.COMMON, CsCosmeticCollection.CLASSIC_PVP));
        list.add(item("cape_minecon", "MineCon Cape", "Classic cape placeholder", CsCosmeticCategory.CAPES, CsCosmeticRarity.RARE, CsCosmeticCollection.CLASSIC_PVP));
        list.add(item("hat_tophat", "Top Hat", "Fancy top hat", CsCosmeticCategory.HATS, CsCosmeticRarity.RARE, CsCosmeticCollection.FANTASY));
        list.add(item("wings_angel", "Angel Wings", "White feathered wings", CsCosmeticCategory.WINGS, CsCosmeticRarity.EPIC, CsCosmeticCollection.FANTASY));
        list.add(item("emote_wave", "Wave", "Friendly wave", CsCosmeticCategory.EMOTES, CsCosmeticRarity.COMMON, CsCosmeticCollection.CS_ORIGINALS));
        list.add(item("emote_clap", "Clap", "Applause", CsCosmeticCategory.EMOTES, CsCosmeticRarity.COMMON, CsCosmeticCollection.CS_ORIGINALS));
        list.add(item("trail_sparkle", "Sparkle Trail", "Sparkle particles", CsCosmeticCategory.TRAILS, CsCosmeticRarity.EPIC, CsCosmeticCollection.NEON));
        list.add(item("banner_cs", "CS Banner", "CS Launcher banner", CsCosmeticCategory.BANNERS, CsCosmeticRarity.RARE, CsCosmeticCollection.CS_ORIGINALS));
        list.add(item("glasses_pixel", "Pixel Glasses", "Retro shades", CsCosmeticCategory.GLASSES, CsCosmeticRarity.COMMON, CsCosmeticCollection.NEON));
        ITEMS = Collections.unmodifiableList(list);
    }

    private static CsCosmeticItem item(String id, String name, String desc,
                                       CsCosmeticCategory cat, CsCosmeticRarity rar, CsCosmeticCollection col) {
        CsCosmeticItem i = new CsCosmeticItem(id, name, desc, cat, rar, col);
        i.free = true;
        return i;
    }

    private CsLocalCosmeticCatalogue() {}

    public static List<CsCosmeticItem> all() {
        return ITEMS;
    }

    public static CsCosmeticItem byId(String id) {
        for (CsCosmeticItem i : ITEMS) {
            if (i.id.equals(id)) return i;
        }
        return null;
    }
}
