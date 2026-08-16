package net.kdt.pojavlaunch;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData;
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.EditorExitable;
import net.kdt.pojavlaunch.customcontrols.LauncherControlImportHost;
import net.kdt.pojavlaunch.customcontrols.handleview.DrawerPullButton;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;


public class CustomControlsActivity extends BaseActivity implements EditorExitable, LauncherControlImportHost {
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerNavigationView;
	private ControlLayout mControlLayout;
    private DrawerPullButton mEditorSettingsButton;
    private ControlInterface mPendingImageControl;
    private final ActivityResultLauncher<String> mMenuImagePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::applyControlButtonImage);
    private final ActivityResultLauncher<String> mDrawerImagePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::applyDrawerButtonImage);
    private final ActivityResultLauncher<String[]> mControlJsonPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::importControlJson);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_custom_controls);

		mControlLayout = findViewById(R.id.customctrl_controllayout);
		mDrawerLayout = findViewById(R.id.customctrl_drawerlayout);
		mDrawerNavigationView = findViewById(R.id.customctrl_navigation_view);
		mEditorSettingsButton = findViewById(R.id.drawer_button);
        View editorCanvas = findViewById(R.id.control_editor_canvas);
        editorCanvas.setAlpha(0f);
        editorCanvas.setTranslationY(10f * getResources().getDisplayMetrics().density);
        editorCanvas.animate().alpha(1f).translationY(0f).setDuration(280)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        mEditorSettingsButton.setAlpha(0f);
        mEditorSettingsButton.animate().alpha(1f).setStartDelay(100).setDuration(220).start();

		// The fixed in-game Settings/Drawer handle is edited directly here.
        // Short tap opens tools; hold + drag moves it anywhere and persists.
        setupSettingsButtonGestures();
		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		String[] menuItems = getResources().getStringArray(R.array.menu_customcontrol_customactivity);
		// Phase-5 premium silver icon set (Req-11) — no more framework glyphs
		int[] menuIcons = new int[]{
				R.drawable.ic_ctrl_add_button,   // Add Button
				R.drawable.ic_ctrl_add_drawer,   // Add Drawer
				R.drawable.ic_ctrl_add_joystick, // Add Joystick
				R.drawable.ic_ctrl_add_command,  // Add Command Button
				R.drawable.ic_ctrl_load,         // Import controls JSON
                R.drawable.ic_menu_settings,     // Settings handle image
                R.drawable.ic_setting_buttonscale, // Settings handle size
				R.drawable.ic_ctrl_save,         // Save
				R.drawable.ic_ctrl_default,      // Default
				R.drawable.ic_ctrl_export        // Export
		};
        final boolean[] menuAnimated = new boolean[menuItems.length];
		mDrawerNavigationView.setAdapter(new ArrayAdapter<String>(this, R.layout.item_custom_control_menu, R.id.menu_item_text, menuItems) {
			@androidx.annotation.NonNull
			@Override
			public View getView(int position, @androidx.annotation.Nullable View convertView, @androidx.annotation.NonNull ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				ImageView icon = view.findViewById(R.id.menu_item_icon);
                ImageView arrow = view.findViewById(R.id.menu_item_arrow);
                TextView text = view.findViewById(R.id.menu_item_text);
                TextView section = view.findViewById(R.id.menu_item_section);
                View card = view.findViewById(R.id.menu_item_card);
				if (icon != null && position < menuIcons.length) icon.setImageResource(menuIcons[position]);
                if (section != null) {
                    section.setVisibility(position == 0 || position == 4 ? View.VISIBLE : View.GONE);
                    if (position == 0) section.setText("CREATE");
                    else if (position == 4) section.setText("LIBRARY");
                }
                boolean importRow = position == 4;
                if (card != null) card.setBackgroundResource(importRow
                        ? R.drawable.bg_skin_button_primary : R.drawable.bg_fastclient_card);
                if (text != null) text.setTextColor(importRow ? 0xFF101014 : 0xFFF2F2F2);
                if (icon != null) icon.setColorFilter(importRow ? 0xFF101014 : 0xFFFFFFFF);
                if (arrow != null) arrow.setColorFilter(importRow ? 0xFF3A3A3A : 0xFF7E8088);
                if (!menuAnimated[position]) {
                    menuAnimated[position] = true;
                    view.setAlpha(0f);
                    view.setTranslationX(14f * getResources().getDisplayMetrics().density);
                    view.animate().alpha(1f).translationX(0f).setStartDelay(position * 30L)
                            .setDuration(240).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                } else {
                    view.setAlpha(1f); view.setTranslationX(0f);
                }
				return view;
			}
		});
		mDrawerNavigationView.setOnItemClickListener((parent, view, position, id) -> {
			android.util.Log.i("CustomControlsActivity", "Menu item clicked: position=" + position);
			switch(position) {
				case 0: android.util.Log.i("CustomControlsActivity", "Action: Add Button"); mControlLayout.addControlButton(new ControlData("New")); break;
				case 1: android.util.Log.i("CustomControlsActivity", "Action: Add Button Drawer"); mControlLayout.addDrawer(new ControlDrawerData()); break;
				case 2: android.util.Log.i("CustomControlsActivity", "Action: Add Joystick"); mControlLayout.addJoystickButton(new ControlJoystickData()); break;
				case 3: {
					android.util.Log.i("CustomControlsActivity", "Action: Add Command Button");
					net.kdt.pojavlaunch.customcontrols.ControlData cmdData = new net.kdt.pojavlaunch.customcontrols.ControlData("Command");
					cmdData.keycodes[0] = net.kdt.pojavlaunch.customcontrols.ControlData.SPECIALBTN_CHATCOMMAND;
					mControlLayout.addControlButton(cmdData);
					// Open the edit dialog for this new button immediately
					final net.kdt.pojavlaunch.customcontrols.ControlData finalCmdData = cmdData;
					mControlLayout.post(() -> {
						for (int i = 0; i < mControlLayout.getChildCount(); i++) {
							android.view.View child = mControlLayout.getChildAt(i);
							if (child instanceof net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface) {
								net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface iface = (net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface) child;
								if (iface.getProperties() == finalCmdData) {
									mControlLayout.editControlButton(iface);
									break;
								}
							}
						}
					});
					break;
				}
				case 4: mControlJsonPicker.launch(new String[]{"application/json", "text/json", "text/plain"}); break;
                case 5: mDrawerImagePicker.launch("image/*"); break;
                case 6: showSettingsButtonSizeDialog(); break;
				case 7: android.util.Log.i("CustomControlsActivity", "Action: Save"); mControlLayout.openSaveDialog(this); break;
				case 8: android.util.Log.i("CustomControlsActivity", "Action: Select Default"); mControlLayout.openSetDefaultDialog(); break;
				case 9: // Saving the currently shown control
					android.util.Log.i("CustomControlsActivity", "Action: Share layout");
					try {
						Uri contentUri = DocumentsContract.buildDocumentUri(getString(R.string.storageProviderAuthorities), mControlLayout.saveToDirectory(mControlLayout.mLayoutFileName));

						Intent shareIntent = new Intent();
						shareIntent.setAction(Intent.ACTION_SEND);
						shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
						shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						shareIntent.setType("application/json");
						startActivity(shareIntent);

						Intent sendIntent = Intent.createChooser(shareIntent, mControlLayout.mLayoutFileName);
						startActivity(sendIntent);
					}catch (Exception e) {
						Tools.showError(this, e);
					}
					break;
			}
			mDrawerLayout.closeDrawers();
		});
		mControlLayout.setModifiable(true);
		try {
			mControlLayout.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH);
		}catch (IOException e) {
			Tools.showError(this, e);
		}
	}

    private void setupSettingsButtonGestures() {
        mEditorSettingsButton.applySavedLayout();
        mEditorSettingsButton.setOnTouchListener(new View.OnTouchListener() {
            float offsetX, offsetY; long downTime; boolean dragging;
            @Override public boolean onTouch(View v, android.view.MotionEvent e) {
                View parent = (View)v.getParent();
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        offsetX = e.getX(); offsetY = e.getY(); downTime = e.getEventTime(); dragging = false;
                        return true;
                    case android.view.MotionEvent.ACTION_MOVE:
                        if (!dragging && e.getEventTime() - downTime >= 220) {
                            dragging = true;
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                        }
                        if (dragging) {
                            int[] loc = new int[2]; parent.getLocationOnScreen(loc);
                            float x = e.getRawX() - loc[0] - offsetX;
                            float y = e.getRawY() - loc[1] - offsetY;
                            v.setX(Math.max(0, Math.min(parent.getWidth() - v.getWidth(), x)));
                            v.setY(Math.max(0, Math.min(parent.getHeight() - v.getHeight(), y)));
                        }
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                        if (dragging) mEditorSettingsButton.savePosition();
                        else mDrawerLayout.openDrawer(GravityCompat.END);
                        return true;
                    case android.view.MotionEvent.ACTION_CANCEL:
                        if (dragging) mEditorSettingsButton.savePosition();
                        return true;
                }
                return true;
            }
        });
    }

    private void showSettingsButtonSizeDialog() {
        String[] sizes = {"Small  44 × 26", "Medium  60 × 30", "Large  92 × 48"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Settings Button Size")
                .setItems(sizes, (d, which) -> {
                    if (which == 0) mEditorSettingsButton.setSizeDp(44, 26);
                    else if (which == 1) mEditorSettingsButton.setSizeDp(60, 30);
                    else mEditorSettingsButton.setSizeDp(92, 48);
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void applyDrawerButtonImage(Uri uri) {
        if (uri == null) return;
        String data = encodeImage(uri);
        if (data == null) { Toast.makeText(this, "Unsupported image", Toast.LENGTH_SHORT).show(); return; }
        mEditorSettingsButton.setCustomImageData(data);
        Toast.makeText(this, "Settings button image changed", Toast.LENGTH_SHORT).show();
    }

    private String encodeImage(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            Bitmap source = BitmapFactory.decodeStream(in);
            if (source == null) return null;
            int max = 192;
            float scale = Math.min(1f, max / (float)Math.max(source.getWidth(), source.getHeight()));
            Bitmap bitmap = scale < 1f ? Bitmap.createScaledBitmap(source,
                    Math.max(1, Math.round(source.getWidth() * scale)),
                    Math.max(1, Math.round(source.getHeight() * scale)), true) : source;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            return "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) { return null; }
    }

    private void importControlJson(Uri uri) {
        if (uri == null) return;
        try {
            new File(Tools.CTRLMAP_PATH).mkdirs();
            String raw = Tools.getFileName(this, uri);
            String name = raw == null ? "imported_controls.json" : raw.replaceAll("[^A-Za-z0-9._-]", "_");
            if (!name.toLowerCase().endsWith(".json")) name += ".json";
            File dest = new File(Tools.CTRLMAP_PATH, name);
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(dest)) {
                if (in == null) throw new IOException("Unable to read selected file");
                byte[] buffer = new byte[8192]; int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            }
            mControlLayout.loadLayout(dest.getAbsolutePath());
            Toast.makeText(this, "Controls imported: " + name, Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Tools.showError(this, e); }
    }

    @Override
    public void requestControlImage(ControlInterface control) {
        mPendingImageControl = control;
        mMenuImagePicker.launch("image/*");
    }

    private void applyControlButtonImage(Uri uri) {
        if (uri == null || mPendingImageControl == null) return;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            Bitmap source = BitmapFactory.decodeStream(in);
            if (source == null) throw new IOException("Unsupported image");
            int max = 192;
            float scale = Math.min(1f, max / (float)Math.max(source.getWidth(), source.getHeight()));
            Bitmap bitmap = scale < 1f ? Bitmap.createScaledBitmap(source,
                    Math.max(1, Math.round(source.getWidth() * scale)),
                    Math.max(1, Math.round(source.getHeight() * scale)), true) : source;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            applyControlIconData("data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP));
            android.widget.Toast.makeText(this, "Control image applied", android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Tools.showError(this, e);
        }
    }

    private void applyControlIconData(String data) {
        ControlInterface control = mPendingImageControl;
        if (control == null) return;
        ControlData properties = control.getProperties();
        properties.customIcon = data;
        control.setProperties(properties);
        mPendingImageControl = null;
    }

	@Override
	public void onBackPressed() {
		mControlLayout.askToExit(this);
	}

	@Override
	public void exitEditor() {
		super.onBackPressed();
	}
}
