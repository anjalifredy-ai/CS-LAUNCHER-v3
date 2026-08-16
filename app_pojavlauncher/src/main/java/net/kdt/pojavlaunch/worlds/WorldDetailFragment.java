package net.kdt.pojavlaunch.worlds;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.UiMotion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;

/** Full-screen world details/editor replacing the old popup card. */
public class WorldDetailFragment extends Fragment {
    public static final String TAG = "WORLD_DETAIL_FRAGMENT";
    public static final String ARG_WORLD = "world_path";
    public static final String ARG_GAME_DIR = "world_game_dir";

    private WorldEntry world;
    private File gameDir;
    private ImageView icon;
    private EditText name;
    private TextView title, meta, status;
    private ProgressBar progress;

    private final ActivityResultLauncher<String> imagePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::changeWorldIcon);

    public WorldDetailFragment() { super(R.layout.fragment_world_detail); }

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        Bundle args = getArguments();
        String path = args != null ? args.getString(ARG_WORLD) : null;
        world = new WorldEntry(new File(path == null ? "" : path));
        String gd = args != null ? args.getString(ARG_GAME_DIR) : null;
        gameDir = new File(gd == null ? world.folder.getParent() : gd);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        icon = view.findViewById(R.id.world_detail_icon);
        name = view.findViewById(R.id.world_detail_name);
        title = view.findViewById(R.id.world_detail_title);
        meta = view.findViewById(R.id.world_detail_meta);
        status = view.findViewById(R.id.world_detail_status);
        progress = view.findViewById(R.id.world_detail_progress);
        ((TextView)view.findViewById(R.id.world_detail_folder)).setText(world.folderName);
        ((TextView)view.findViewById(R.id.world_detail_path)).setText(world.folder.getAbsolutePath());
        view.findViewById(R.id.world_detail_back).setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        view.findViewById(R.id.world_detail_change_icon).setOnClickListener(v -> imagePicker.launch("image/*"));
        view.findViewById(R.id.world_detail_rename).setOnClickListener(v -> renameWorld());
        view.findViewById(R.id.world_detail_backup).setOnClickListener(v -> backupWorld());
        view.findViewById(R.id.world_detail_browse).setOnClickListener(v -> copyPath());
        UiMotion.pressFeedback(view.findViewById(R.id.world_detail_back),
                view.findViewById(R.id.world_detail_change_icon), view.findViewById(R.id.world_detail_rename),
                view.findViewById(R.id.world_detail_backup), view.findViewById(R.id.world_detail_browse));
        loadWorld();
        UiMotion.revealScreen(view);
    }

    private void loadWorld() {
        setBusy(true, "SCANNING");
        PojavApplication.sExecutorService.execute(() -> {
            WorldOps.enrich(Collections.singletonList(world));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                title.setText(world.displayName);
                name.setText(world.displayName);
                meta.setText("Version  " + value(world.versionName) + "\nLast played  "
                        + WorldOps.formatLastPlayed(world.lastPlayedMs) + "\nSize  "
                        + WorldOps.formatSize(world.sizeBytes) + "\nSeed  "
                        + (world.hasSeed ? String.valueOf(world.seed) : "—") + "\nDatapacks  "
                        + (world.datapackCount >= 0 ? world.datapackCount : 0));
                loadIcon();
                setBusy(false, "READY");
            });
        });
    }

    private void loadIcon() {
        File file = world.iconFile();
        Bitmap bitmap = file != null ? BitmapFactory.decodeFile(file.getAbsolutePath()) : null;
        if (bitmap != null) { icon.clearColorFilter(); icon.setImageBitmap(bitmap); }
        else { icon.setImageResource(R.drawable.ic_nav_worlds); icon.setColorFilter(0xFF777984); }
    }

    private void renameWorld() {
        String next = name.getText() == null ? "" : name.getText().toString().trim();
        if (next.isEmpty() || next.equals(world.displayName)) return;
        setBusy(true, "RENAMING");
        WorldOps.renameWorld(world, next, callback(true));
    }

    private void backupWorld() {
        setBusy(true, "BACKING UP");
        WorldOps.backupWorld(world, gameDir, callback(false));
    }

    private WorldOps.OpCallback callback(boolean leaveAfter) {
        return new WorldOps.OpCallback() {
            @Override public void onProgress(int pct, @Nullable String message) {
                if (!isAdded()) return;
                status.setText(message == null ? "WORKING" : message.toUpperCase());
            }
            @Override public void onDone(boolean ok, @NonNull String message) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                setBusy(false, ok ? "SAVED" : "FAILED");
                if (ok && leaveAfter) requireActivity().getSupportFragmentManager().popBackStack();
            }
        };
    }

    private void copyPath() {
        ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("world", world.folder.getAbsolutePath()));
        Toast.makeText(requireContext(), "World path copied", Toast.LENGTH_SHORT).show();
    }

    private void changeWorldIcon(Uri uri) {
        if (uri == null) return;
        setBusy(true, "SAVING IMAGE");
        PojavApplication.sExecutorService.execute(() -> {
            boolean ok = false;
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                Bitmap source = BitmapFactory.decodeStream(in);
                if (source == null) throw new Exception("Unsupported image");
                int side = Math.min(source.getWidth(), source.getHeight());
                int left = (source.getWidth() - side) / 2;
                int top = (source.getHeight() - side) / 2;
                Bitmap output = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(output);
                canvas.drawBitmap(source, new Rect(left, top, left + side, top + side),
                        new Rect(0, 0, 128, 128), null);
                try (FileOutputStream out = new FileOutputStream(new File(world.folder, "icon.png"))) {
                    output.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                ok = true;
            } catch (Exception ignored) {}
            final boolean saved = ok;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                setBusy(false, saved ? "IMAGE SAVED" : "IMAGE FAILED");
                if (saved) loadIcon();
                Toast.makeText(requireContext(), saved ? "World image changed" : "Could not use this image", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void setBusy(boolean busy, String text) {
        if (progress != null) progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (status != null) status.setText(text);
    }

    private static String value(String text) { return text == null || text.isEmpty() ? "—" : text; }
}
