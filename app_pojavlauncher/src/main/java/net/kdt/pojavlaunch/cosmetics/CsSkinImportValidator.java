package net.kdt.pojavlaunch.cosmetics;

import android.graphics.BitmapFactory;

import java.io.File;

/**
 * Validates Minecraft skin PNG dimensions (64x64, 64x32 legacy, 128x128 HD).
 */
public final class CsSkinImportValidator {

    private static final long MAX_BYTES = 2L * 1024 * 1024;

    public static class Result {
        public final boolean ok;
        public final String message;
        public final int width;
        public final int height;
        public final String modelHint;

        public Result(boolean ok, String message, int width, int height, String modelHint) {
            this.ok = ok;
            this.message = message;
            this.width = width;
            this.height = height;
            this.modelHint = modelHint;
        }
    }

    private CsSkinImportValidator() {}

    public static Result validate(File file) {
        if (file == null || !file.exists()) {
            return new Result(false, "File not found", 0, 0, null);
        }
        if (file.length() > MAX_BYTES) {
            return new Result(false, "File too large (max 2 MB)", 0, 0, null);
        }
        if (!file.getName().toLowerCase().endsWith(".png")) {
            return new Result(false, "Only PNG skins are supported", 0, 0, null);
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        int w = opts.outWidth;
        int h = opts.outHeight;
        if (w <= 0 || h <= 0) {
            return new Result(false, "Not a valid image", 0, 0, null);
        }

        if (w == 64 && h == 64) {
            return new Result(true, "Valid 64×64 skin", w, h, "classic/slim");
        }
        if (w == 64 && h == 32) {
            return new Result(true, "Valid legacy 64×32 skin", w, h, "classic");
        }
        if (w == 128 && h == 128) {
            return new Result(true, "Valid HD 128×128 skin", w, h, "classic/slim");
        }
        return new Result(false,
                "Unsupported size " + w + "×" + h + ". Use 64×64, 64×32, or 128×128 PNG.",
                w, h, null);
    }
}
