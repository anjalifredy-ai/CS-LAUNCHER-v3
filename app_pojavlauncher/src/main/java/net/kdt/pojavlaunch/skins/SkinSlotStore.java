package net.kdt.pojavlaunch.skins;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

/** Exactly three local skin presets for one Minecraft account. */
public final class SkinSlotStore {
    public static final int SLOT_COUNT = 3;

    public static final class Slot {
        public final int index;
        public String name;
        public String model;
        public long updatedAt;
        Slot(int index) { this.index = index; this.name = "Skin Slot " + (index + 1); this.model = "classic"; }
    }

    private final MinecraftAccount account;
    private final File root;
    private final File metadataFile;
    private final Slot[] slots = new Slot[SLOT_COUNT];
    private int activeSlot = -1;

    public SkinSlotStore(@NonNull Context context, @NonNull MinecraftAccount account) {
        this.account = account;
        String accountKey = account.profileId != null && !account.profileId.trim().isEmpty()
                ? account.profileId : account.username;
        accountKey = accountKey == null ? "default" : accountKey.replaceAll("[^a-zA-Z0-9._-]", "_");
        root = new File(Tools.DIR_DATA, "skin_library/" + accountKey);
        metadataFile = new File(root, "slots.json");
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = new Slot(i);
        //noinspection ResultOfMethodCallIgnored
        root.mkdirs();
        load();
        migrateCurrentSkin();
    }

    public Slot get(int index) { return slots[index]; }
    public int getActiveSlot() { return activeSlot; }
    public boolean isFilled(int index) { File f = fileFor(index); return f.isFile() && f.length() > 0; }
    public File fileFor(int index) { return new File(root, "slot_" + (index + 1) + ".png"); }

    public void saveSlot(int index, @NonNull File source, @Nullable String displayName, boolean slim) throws Exception {
        if (index < 0 || index >= SLOT_COUNT || !source.isFile()) throw new IllegalArgumentException("Invalid skin slot");
        File temp = new File(root, "slot_" + (index + 1) + ".tmp");
        FileUtils.copyFile(source, temp);
        File target = fileFor(index);
        if (target.exists() && !target.delete()) throw new Exception("Could not replace slot");
        if (!temp.renameTo(target)) { FileUtils.copyFile(temp, target); temp.delete(); }
        Slot slot = slots[index];
        slot.name = displayName == null || displayName.trim().isEmpty()
                ? "Skin Slot " + (index + 1) : displayName.trim();
        slot.model = slim ? "slim" : "classic";
        slot.updatedAt = System.currentTimeMillis();
        save();
    }

    public void activate(int index) throws Exception {
        if (!isFilled(index)) throw new IllegalArgumentException("Skin slot is empty");
        File skinsDir = new File(Tools.DIR_DATA, "skins");
        //noinspection ResultOfMethodCallIgnored
        skinsDir.mkdirs();
        File active = new File(skinsDir, account.username + "_skin.png");
        File temp = new File(skinsDir, account.username + "_skin.tmp");
        FileUtils.copyFile(fileFor(index), temp);
        if (active.exists() && !active.delete()) throw new Exception("Could not replace active skin");
        if (!temp.renameTo(active)) { FileUtils.copyFile(temp, active); temp.delete(); }
        Tools.write(new File(skinsDir, account.username + "_metadata.json").getAbsolutePath(),
                "{\n  \"model\": \"" + slots[index].model + "\",\n  \"slot\": " + (index + 1) + "\n}");
        activeSlot = index;
        save();
        account.clearFaceCache();
    }

    public void deleteSlot(int index) {
        if (index < 0 || index >= SLOT_COUNT) return;
        //noinspection ResultOfMethodCallIgnored
        fileFor(index).delete();
        slots[index] = new Slot(index);
        if (activeSlot == index) {
            activeSlot = -1;
            File skinsDir = new File(Tools.DIR_DATA, "skins");
            //noinspection ResultOfMethodCallIgnored
            new File(skinsDir, account.username + "_skin.png").delete();
            //noinspection ResultOfMethodCallIgnored
            new File(skinsDir, account.username + "_metadata.json").delete();
            account.clearFaceCache();
        }
        save();
    }

    public void clearAll() {
        for (int i = 0; i < SLOT_COUNT; i++) deleteSlot(i);
    }

    private void migrateCurrentSkin() {
        if (metadataFile.exists()) return;
        File current = new File(Tools.DIR_DATA + "/skins/" + account.username + "_skin.png");
        if (current.isFile()) {
            try {
                boolean slim = false;
                File meta = new File(Tools.DIR_DATA + "/skins/" + account.username + "_metadata.json");
                if (meta.isFile()) slim = Tools.read(meta.getAbsolutePath()).toLowerCase().contains("slim");
                saveSlot(0, current, "Current Skin", slim);
                activeSlot = 0;
                save();
            } catch (Exception ignored) {}
        } else save();
    }

    private void load() {
        if (!metadataFile.isFile()) return;
        try {
            JSONObject rootJson = new JSONObject(Tools.read(metadataFile.getAbsolutePath()));
            activeSlot = rootJson.optInt("activeSlot", -1);
            JSONArray array = rootJson.optJSONArray("slots");
            if (array != null) {
                for (int i = 0; i < Math.min(array.length(), SLOT_COUNT); i++) {
                    JSONObject o = array.optJSONObject(i); if (o == null) continue;
                    slots[i].name = o.optString("name", "Skin Slot " + (i + 1));
                    slots[i].model = o.optString("model", "classic");
                    slots[i].updatedAt = o.optLong("updatedAt", 0L);
                }
            }
        } catch (Exception ignored) { activeSlot = -1; }
    }

    private void save() {
        try {
            JSONObject o = new JSONObject(); o.put("version", 1); o.put("activeSlot", activeSlot);
            JSONArray array = new JSONArray();
            for (Slot slot : slots) {
                JSONObject s = new JSONObject(); s.put("index", slot.index); s.put("name", slot.name);
                s.put("model", slot.model); s.put("updatedAt", slot.updatedAt); s.put("filled", isFilled(slot.index));
                array.put(s);
            }
            o.put("slots", array);
            File temp = new File(root, "slots.tmp"); Tools.write(temp.getAbsolutePath(), o.toString(2));
            if (metadataFile.exists()) metadataFile.delete();
            if (!temp.renameTo(metadataFile)) { FileUtils.copyFile(temp, metadataFile); temp.delete(); }
        } catch (Exception ignored) {}
    }
}
