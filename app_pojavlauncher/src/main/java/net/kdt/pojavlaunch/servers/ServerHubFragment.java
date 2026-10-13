package net.kdt.pojavlaunch.servers;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.util.ArrayList;
import java.util.List;

public class ServerHubFragment extends Fragment {

    public static final String TAG = "ServerHubFragment";

    private ServerAdapter adapter;
    private final List<ServerEntry> data = new ArrayList<>();
    private String profileKey;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_server_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileKey = LauncherPreferences.DEFAULT_PREF.getString(
                LauncherPreferences.PREF_KEY_CURRENT_PROFILE, "default");

        RecyclerView rv = view.findViewById(R.id.rv_servers);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new ServerAdapter(data, new ServerAdapter.Listener() {
                @Override
                public void onJoin(ServerEntry e) {
                    if (e == null || e.address == null) return;
                    Toast.makeText(requireContext(), "Joining " + e.address, Toast.LENGTH_SHORT).show();
                    ServerStore.setPending(requireContext(), profileKey, e.address);
                }

                @Override
                public void onEdit(ServerEntry e) {
                    showEditDialog(e);
                }

                @Override
                public void onRemove(ServerEntry e) {
                    if (e == null) return;
                    ServerStore.remove(requireContext(), profileKey, e.address);
                    reload();
                }
            });
            rv.setAdapter(adapter);
        }

        View add = view.findViewById(R.id.btn_add_server);
        if (add != null) add.setOnClickListener(v -> showAddDialog());

        View back = view.findViewById(R.id.btn_back);
        if (back != null) back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        reload();
    }

    private void showAddDialog() {
        EditText name = new EditText(requireContext());
        name.setHint("Name");
        EditText addr = new EditText(requireContext());
        addr.setHint("address:port");
        android.widget.LinearLayout box = new android.widget.LinearLayout(requireContext());
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(p, p, p, p);
        box.addView(name);
        box.addView(addr);
        new AlertDialog.Builder(requireContext())
                .setTitle("Add server")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    String n = name.getText().toString().trim();
                    String a = addr.getText().toString().trim();
                    if (a.isEmpty()) {
                        Toast.makeText(requireContext(), "Address required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ServerEntry e = new ServerEntry();
                    e.name = n.isEmpty() ? a : n;
                    e.address = a;
                    ServerStore.add(requireContext(), profileKey, e);
                    reload();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(ServerEntry e) {
        if (e == null) return;
        EditText name = new EditText(requireContext());
        name.setText(e.name);
        EditText addr = new EditText(requireContext());
        addr.setText(e.address);
        android.widget.LinearLayout box = new android.widget.LinearLayout(requireContext());
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(p, p, p, p);
        box.addView(name);
        box.addView(addr);
        new AlertDialog.Builder(requireContext())
                .setTitle("Edit server")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    e.name = name.getText().toString().trim();
                    e.address = addr.getText().toString().trim();
                    ServerStore.update(requireContext(), profileKey, e);
                    reload();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reload() {
        data.clear();
        data.addAll(ServerStore.load(requireContext(), profileKey));
        if (adapter != null) adapter.notifyDataSetChanged();
    }
}
