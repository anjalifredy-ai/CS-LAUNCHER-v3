package net.kdt.pojavlaunch.servers;

// TEMP minimal compile-safe stub — full file restored next
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import net.kdt.pojavlaunch.R;

public class ServerDetailFragment extends Fragment {
    public static final String TAG = "ServerDetailFragment";
    public static final String ARG_ID = "serverId";

    public ServerDetailFragment() { super(R.layout.fragment_server_detail); }

    public static Bundle args(String serverId) {
        Bundle b = new Bundle();
        b.putString(ARG_ID, serverId);
        return b;
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle saved) {
        super.onViewCreated(root, saved);
        Toast.makeText(requireContext(), "Loading server UI…", Toast.LENGTH_SHORT).show();
        // Full dashboard restored in follow-up commit
        try {
            requireActivity().getSupportFragmentManager().popBackStack();
        } catch (Throwable ignored) {}
    }
}
