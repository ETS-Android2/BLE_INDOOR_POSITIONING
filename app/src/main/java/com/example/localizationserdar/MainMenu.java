package com.example.localizationserdar;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.localizationserdar.databinding.MainMenuBinding;
import com.example.localizationserdar.datamanager.DataManager;
import com.example.localizationserdar.datamodels.User;
import com.example.localizationserdar.utils.OnboardingUtils;
import com.github.florent37.tutoshowcase.TutoShowcase;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Calendar;
import java.util.LinkedList;

import static com.example.localizationserdar.utils.Constants.COLLECTION_USERS;
import static com.example.localizationserdar.utils.Constants.EMPTY_STRING;
import static com.example.localizationserdar.utils.Constants.EXISTING_USER;
import static com.example.localizationserdar.utils.Constants.STATUS_ACCEPTED;
import static com.example.localizationserdar.utils.Constants.STATUS_PENDING;
import static com.example.localizationserdar.utils.Constants.STATUS_REJECTED;
import static com.example.localizationserdar.utils.Constants.USER_STATUS;
import static com.example.localizationserdar.utils.Constants.VERIFICATION_STATUS;

public class MainMenu extends Fragment implements NavigationView.OnNavigationItemSelectedListener {

    private MainMenuBinding binding;
    private ListenerRegistration modStatusListener;

    User user;

    private static final String TAG = "DEBUGGING...";

    public MainMenu() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (modStatusListener != null) {
            modStatusListener.remove();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        setNavigationViewListener();
        binding = MainMenuBinding.inflate(inflater, container, false);
        ((OnboardingUtils) requireActivity()).hideToolbar();

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        DataManager.getInstance().getBeacons((beacons, exception) -> {
            if (beacons != null) {
                LocalizationLevel.getInstance().allBeacons = beacons;
            } else {
                LocalizationLevel.getInstance().allBeacons = new LinkedList<>();
            }
        });

        if (getArguments() != null && getArguments().getString(USER_STATUS, EMPTY_STRING).equals(EXISTING_USER)) {
            Log.d("Hello, Serdar, ", "How are you?");
            DataManager.getInstance().getCurrentUser(
                    (user, exception) -> {
                        if (user != null) {
                            LocalizationLevel.getInstance().currentUser = user;
                        }
                    });
        }

        user = LocalizationLevel.getInstance().currentUser;

        NavigationView navigationView = requireView().findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        setNavDrawer(toolbar);

        //Setting the Greetings message
        setGreetingsText();

        //Display moderation overlay (in case pending/declined)
        manageModerationStatus();
    }

    private void setNavDrawer(Toolbar toolbar) {
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(getActivity(), binding.drawerLayout, toolbar, R.string.drawer_controller_open, R.string.drawer_controller_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();
    }

    private void showVerificationStatusOverlay(Boolean booleanType) {
        TutoShowcase verificationOverlay = TutoShowcase.from(requireActivity());
        if (booleanType) {
            verificationOverlay.setContentView(R.layout.verification_status_overlay)
                    .onClickContentView(R.id.container_overlay_complete, null)
                    .show();
        } else {
            verificationOverlay.setContentView(R.layout.verification_status_overlay)
                    .dismiss();
        }
    }

    private void manageModerationStatus() {
        modStatusListener = FirebaseFirestore.getInstance().collection(COLLECTION_USERS).document(user.userId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.d(TAG, error.toString());
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        user.verificationStatus = documentSnapshot.getString(VERIFICATION_STATUS);
                        if (user.verificationStatus != null) {
                            switch (user.verificationStatus) {
                                case STATUS_REJECTED:
                                case STATUS_PENDING:
                                    showVerificationStatusOverlay(true);
                                    binding.bottomSheet.bSh.setVisibility(View.GONE);
                                    break;
                                case STATUS_ACCEPTED:
                                    showVerificationStatusOverlay(false);
                                    binding.bottomSheet.bSh.setVisibility(View.VISIBLE);
                                    break;

                            }
                        }
                    }
                });
    }

    private void setGreetingsText() {
        View header = binding.navView.getHeaderView(0);
        TextView tvGreetings = header.findViewById(R.id.tv_morning);
        TextView tvName  = header.findViewById(R.id.tv_name);

        user = LocalizationLevel.getInstance().currentUser;
        tvName.setText(user.firstName);

        Calendar rightNow = Calendar.getInstance();
        int timeOfDay = rightNow.get(Calendar.HOUR_OF_DAY);
        Log.d("The time is: ", String.valueOf(timeOfDay));
        if (timeOfDay < 12) {
            tvGreetings.setText(getResources().getString(R.string.tv_morning));
        } else if (timeOfDay < 16) {
            tvGreetings.setText(getResources().getString(R.string.tv_afternoon));
        } else if (timeOfDay < 21) {
            tvGreetings.setText(getResources().getString(R.string.tv_evening));
        } else if (timeOfDay < 24){
            tvGreetings.setText(getResources().getString(R.string.tv_night));
        }
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Navigation.findNavController(requireView()).navigate(R.id.action_mainMenu_to_login);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_profile:
                Navigation.findNavController(requireView()).navigate(R.id.action_mainMenu_to_settings);
                Log.d("Hello," ,"You pressed me!");
                break;
            case R.id.menu_localization:
                //Logic here
                Navigation.findNavController(requireView()).navigate(R.id.action_mainMenu_to_localizationOverview);
                Log.d(TAG, "Hello, you pressed Localization menu");
                break;
            case R.id.menu_language:
                //Logic here
                Log.d(TAG, "Hey, you pressed Logout menu");
                break;
            case R.id.menu_logout:
                signOut();
                break;

        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}