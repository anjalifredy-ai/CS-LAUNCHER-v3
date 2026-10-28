package net.kdt.pojavlaunch.cosmetics;

import java.util.ArrayList;
import java.util.List;

public class CsEquippedLoadout {
    public String skinId;
    public String capeId;
    public String hatId;
    public String backId;
    public String wingsId;
    public List<String> emoteSlots = new ArrayList<>();

    public CsEquippedLoadout() {
        for (int i = 0; i < 6; i++) emoteSlots.add(null);
    }
}
