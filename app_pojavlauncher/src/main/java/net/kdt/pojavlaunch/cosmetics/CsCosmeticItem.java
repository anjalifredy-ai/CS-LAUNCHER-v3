package net.kdt.pojavlaunch.cosmetics;

public class CsCosmeticItem {
    public String id;
    public String name;
    public String description;
    public CsCosmeticCategory category;
    public CsCosmeticRarity rarity;
    public CsCosmeticCollection collection;
    public boolean animated;
    public boolean free = true;
    public String thumbnailPath;
    public String assetPath;
    /** classic | slim */
    public String skinModel;

    public CsCosmeticItem() {}

    public CsCosmeticItem(String id, String name, String description,
                          CsCosmeticCategory category, CsCosmeticRarity rarity,
                          CsCosmeticCollection collection) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.rarity = rarity;
        this.collection = collection;
    }
}
