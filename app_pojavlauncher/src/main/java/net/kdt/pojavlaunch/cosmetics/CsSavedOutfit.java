package net.kdt.pojavlaunch.cosmetics;

public class CsSavedOutfit {
    public String id;
    public String name;
    public CsEquippedLoadout loadout;
    public long updatedAt = System.currentTimeMillis();

    public CsSavedOutfit() {}

    public CsSavedOutfit(String id, String name, CsEquippedLoadout loadout) {
        this.id = id;
        this.name = name;
        this.loadout = loadout;
    }
}
