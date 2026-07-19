package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.dialogOnUiThread;
import static net.kdt.pojavlaunch.Tools.hasNoOnlineProfileDialog;
import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.runOnUiThread;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;
import net.kdt.pojavlaunch.value.MinecraftAccount;
import net.kdt.pojavlaunch.PojavProfile;

import java.io.File;

/**
 * Parrot Launcher - genuine from-scratch home screen.
 * Fires the engine-agnostic launch signal (ExtraCore.LAUNCH_GAME) and routes to the
 * existing profile/account/settings screens. No Amethyst view IDs are reused.
 */
public class ParrotHomeFragment extends Fragment {
    public static final String TAG = "ParrotHomeFragment";

    private mcVersionSpinner mVersionSpinner;

    public ParrotHomeFragment() {
        super(R.layout.fragment_parrot_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mWikiButton = view.findViewById(R.id.parrot_wiki);
        Button mDiscordButton = view.findViewById(R.id.parrot_discord);
        Button mControlsButton = view.findViewById(R.id.parrot_controls);
        Button mJarButton = view.findViewById(R.id.parrot_jar);
        Button mLogsButton = view.findViewById(R.id.parrot_logs);
        Button mFilesButton = view.findViewById(R.id.parrot_files);

        ImageButton mEditProfileButton = view.findViewById(R.id.parrot_edit_profile);
        Button mPlayButton = view.findViewById(R.id.parrot_play);
        mVersionSpinner = view.findViewById(R.id.parrot_version_spinner);

        ImageView mDiscordIcon = view.findViewById(R.id.parrot_discord_icon);
        ImageView mStatusAvatar = view.findViewById(R.id.parrot_status_avatar);
        ImageView mProfileAvatar = view.findViewById(R.id.parrot_profile_avatar);
        TextView mStatusUsername = view.findViewById(R.id.parrot_status_username);
        TextView mProfileUsername = view.findViewById(R.id.parrot_profile_username);

        // ----- show the logged-in account's username + skin face -----
        MinecraftAccount account = PojavProfile.getCurrentProfileContent(requireContext(), null);
        String loginName = (account != null && account.username != null) ? account.username : getString(R.string.profile_default_user);
        mStatusUsername.setText(loginName);
        mProfileUsername.setText(loginName);
        try {
            android.graphics.Bitmap skin = MinecraftAccount.getSkinFace(loginName);
            if (skin != null) {
                mStatusAvatar.setImageBitmap(skin);
                mProfileAvatar.setImageBitmap(skin);
            }
        } catch (Exception ignored) {}

        TextView navProfiles = view.findViewById(R.id.parrot_nav_profiles);
        TextView navAccounts = view.findViewById(R.id.parrot_nav_accounts);
        TextView navLibrary = view.findViewById(R.id.parrot_nav_library);
        TextView navSettings = view.findViewById(R.id.parrot_nav_settings);

        // ----- nav -----
        if (navProfiles != null) navProfiles.setOnClickListener(v -> Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class, ProfileTypeSelectFragment.TAG, null));
        if (navAccounts != null) navAccounts.setOnClickListener(v -> Tools.swapFragment(requireActivity(), SelectAuthFragment.class, SelectAuthFragment.TAG, null));
        if (navSettings != null) navSettings.setOnClickListener(v -> Tools.swapFragment(requireActivity(), LauncherPreferenceFragment.class, "LauncherPreferenceFragment", null));
        if (navLibrary != null) navLibrary.setOnClickListener(v -> openModBrowser(null));

        // ----- tools -----
        mWikiButton.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
        mDiscordButton.setOnClickListener(v -> Tools.openURL(requireActivity(), "https://dsc.gg/parrotclient"));
        if (mDiscordIcon != null) mDiscordIcon.setOnClickListener(v -> Tools.openURL(requireActivity(), "https://dsc.gg/parrotclient"));
        mControlsButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        mJarButton.setOnClickListener(v -> runInstallerWithConfirmation(false));
        mJarButton.setOnLongClickListener(v -> { runInstallerWithConfirmation(true); return true; });
        mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));

        mPlayButton.setOnClickListener(v -> {
            // Only block Sodium on GPUs actually affected by the render-distance issue
            // (Adreno + OpenGL ES 3, used with the LTW renderer). On other GPUs (e.g. Mali)
            // Sodium works fine and should not be force-removed.
            if (Tools.hasMods("sodium") && Tools.affectedByLTWRenderDistanceIssue()
                    && !(LauncherPreferences.DEFAULT_PREF.getBoolean("sodium_override", false))) {
                AlertDialog sodiumWarningDialog = new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.sodium_warning_title)
                        .setMessage(R.string.sodium_warning_message)
                        .setNeutralButton(R.string.delete_sodium, (d, w) -> {
                            Tools.deleteSodiumMods();
                            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
                        })
                        .create();
                sodiumWarningDialog.show();
            } else ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        });

        mLogsButton.setOnClickListener((v) -> shareLog(requireContext()));
        mFilesButton.setOnClickListener((v) -> {
            if (Tools.isDemoProfile(v.getContext())) {
                hasNoOnlineProfileDialog(getActivity(), getString(R.string.demo_unsupported), getString(R.string.change_account));
            } else openPath(v.getContext(), getCurrentProfileDirectory(), false);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVersionSpinner != null) mVersionSpinner.reloadProfiles();
    }

    private File getCurrentProfileDirectory() {
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        if (!Tools.isValidString(currentProfile)) return new File(Tools.DIR_GAME_NEW);
        LauncherProfiles.load();
        MinecraftProfile profileObject = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        if (profileObject == null) return new File(Tools.DIR_GAME_NEW);
        return Tools.getGameDirPath(profileObject);
    }

    private void runInstallerWithConfirmation(boolean isCustomArgs) {
        if (ProgressKeeper.getTaskCount() == 0)
            Tools.installMod(requireActivity(), isCustomArgs);
        else
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }

    /**
     * Open the existing Modrinth / CurseForge browser (SearchModFragment) — the "Library".
     * First asks which profile to install into, then switches to that profile so the
     * installed mod / resource pack / shader / world lands in the right place.
     * @param category optional category hint ("mod", "resourcepack", "shader", "world")
     */
    private void openModBrowser(String category) {
        // Open the Library immediately — the category tabs live there.
        // The profile picker is shown on-demand (when installing), not as a gate,
        // so a missing/empty profile list can never block the Library from showing.
        Tools.swapFragment(requireActivity(), SearchModFragment.class, SearchModFragment.TAG, null);
    }
}
