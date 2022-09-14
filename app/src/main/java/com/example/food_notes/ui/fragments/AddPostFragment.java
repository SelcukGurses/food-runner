package com.example.food_notes.ui.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.food_notes.R;
import com.example.food_notes.databinding.FragmentAddPostBinding;
import com.example.food_notes.injection.Injection;
import com.example.food_notes.ui.view.factory.FoodPostModelViewFactory;
import com.example.food_notes.ui.view.model.FoodPostViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;

//TODO IMPLEMENT PERMISSIONS FUNCTIONALITY LATER
public class AddPostFragment extends Fragment implements AddImageOptionsDialogFragment.AddImageOptionsListener {

    private static final String TAG = "add_post";
    private static int USER_ID;

    // view binding
    private FragmentAddPostBinding binding;

    // views
    private RatingBar ratingBar;
    private FloatingActionButton fab_create, fab_back, fab_choose_image;
    private EditText title;
    private EditText description;
    private ImageView imageView;

    // view model components
    private FoodPostModelViewFactory mFactory;
    private FoodPostViewModel mViewModel;

    // navigation components
    private NavHostFragment navHostFragment;
    private NavController navController;
    private SavedStateHandle savedStateHandle;
    private NavBackStackEntry navBackStackEntry;

    // dialog window
    private AddImageOptionsDialogFragment dialogFragment;

    // result launcher is required to choose image
    private ActivityResultLauncher<Intent> resultLauncher;

    public AddPostFragment() {}

    @Nullable
    public static AddPostFragment newInstance(String requestKey) {
        AddPostFragment fragment = new AddPostFragment();
        Bundle args = new Bundle();
        args.putString("requestKey", requestKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //init view model
        mFactory = Injection.provideFoodPostViewModelFactory(requireActivity().getApplicationContext());
        mViewModel = mFactory.create(FoodPostViewModel.class);
        navController = NavHostFragment.findNavController(this);

        navBackStackEntry = navController.getPreviousBackStackEntry();
        savedStateHandle = navBackStackEntry.getSavedStateHandle();
        if (savedStateHandle.contains("LOGGED_USERID")) {
            USER_ID = savedInstanceState.getInt("LOGGED_USERID");
        }

        registerIntentToGalleryLauncher();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //init view binding
        binding = FragmentAddPostBinding.inflate(inflater, container, false);
        title = binding.tilTitlePost.getEditText();
        description = binding.tilPost.getEditText();
        fab_back = binding.fabBackToMain;
        fab_create = binding.fabCreatePost;
        fab_choose_image = binding.fabChooseImage;
        ratingBar = binding.ratingBarAddPost;
        imageView = binding.ivSetImage;

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fab_choose_image.setOnClickListener(v -> {
            showDialogFragment();

        });
        if (isFilledAllRequiredFields()) {
            fab_create.setOnClickListener(v -> {
                mViewModel.addItem(
                        USER_ID,
                        title.getText().toString(),
                        description.getText().toString(), 
                        ratingBar.getRating()
                );
                requireActivity().runOnUiThread(() -> Toast.makeText(requireActivity().getApplicationContext(), "Yeah", Toast.LENGTH_SHORT).show());
                backToUserMainFragment();
            });
        }
        fab_back.setOnClickListener(v -> backToUserMainFragment());
    }

    private void showDialogFragment() {
        dialogFragment = new AddImageOptionsDialogFragment();
        dialogFragment.show(getChildFragmentManager(), AddImageOptionsDialogFragment.TAG);
    }

    private void captureImageWithCamera() {
        Toast.makeText(requireActivity().getApplicationContext(), "Go capture an image!", Toast.LENGTH_SHORT).show();
    }

    private void chooseImageFromGallery() throws IOException {
        Intent intentToChooseImage = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        resultLauncher.launch(intentToChooseImage);
        //startActivity(intentToChooseImage);
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Bitmap thumbnail = getContext().getContentResolver().loadThumbnail(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new Size(480, 240), null);
            imageView.setImageBitmap(thumbnail);
            }*/

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    /**
     * Prompts user to allow entry permission to local storage.
     */
    private void getPermissionForGallery() {
        Dexter.withContext(requireActivity()).withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        try {
                            chooseImageFromGallery();
                        } catch (IOException e) {
                            Log.d(TAG, "onPermissionGranted: "+e.getLocalizedMessage());
                        }
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        showRationaleDialogForPermissions();
                    }
                }).withErrorListener(dexterError -> Toast.makeText(requireActivity().getApplicationContext(), "Error - "+dexterError.toString(), Toast.LENGTH_SHORT).show())
                .onSameThread().check();
    }

    private void getPermissionForCamera() {
        Dexter.withContext(requireActivity()).withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        captureImageWithCamera();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        showRationaleDialogForPermissions();
                    }
                }).withErrorListener(dexterError -> Snackbar.make(this.requireContext(), this.requireView(), "E", Snackbar.LENGTH_SHORT));
    }

    private void showRationaleDialogForPermissions() {
        new AlertDialog.Builder(requireActivity())
                .setMessage("You are required to turn on permissions from settings")
                .setPositiveButton("To Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", intent.getPackage(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.e("ERROR", e.getLocalizedMessage());
                        }
                    }
                }).setNegativeButton("Dismiss", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Initializes some of the attributes of the rating bar in the add_post fragment.
     */
    private void initRatingBar() {
            binding.ratingBarAddPost.setNumStars(5);
            binding.ratingBarAddPost.setIsIndicator(false);
            binding.ratingBarAddPost.setRating(0);
    }

    /**
     * Navigates user back to UserMainFragment
     */
    private void backToUserMainFragment() {
        navController.navigate(R.id.userMainFragment);
    }

    /**
     * if any field(title,desc) is empty return false, otherwise true.
     * @return bool
     */
    private boolean isFilledAllRequiredFields() {
        if (TextUtils.isEmpty(title.getText()) &&
        TextUtils.isEmpty(description.getText())) {
            title.setError("Title is required");
            description.setError("Description is required");
            return false;
        }
        if (TextUtils.isEmpty(title.getText())) {
            title.setError("Title is required");
            return false;
        }
        if (TextUtils.isEmpty(description.getText())) {
            description.setError("Description is required");
            return false;
        }
        return true;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Calls on DialogListener to listen and handle the click event for storage option.
     * @param dialog AddImageOptionsDialogFragment
     */
    @Override
    public void onDialogStorageOptionClick(DialogFragment dialog) {
        Log.d(TAG, "onDialogStorageOptionClick: storage");
        getPermissionForGallery();
    }

    /**
     * Calls on DialogListener to listen and handle the click event for camera option
     * @param dialog AddImageOptionsDialogFragment
     */
    @Override
    public void onDialogCameraOptionClick(DialogFragment dialog) {
        // TODO implement functions
        Log.d(TAG, "onDialogCameraOptionClick: camera");
        getPermissionForCamera();
    }

    private void registerIntentToGalleryLauncher() {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Uri uri = result.getResultCode() == Activity.RESULT_OK && result.getData() != null ? result.getData().getData() : null;
                        try {
                            final Bitmap bm = mViewModel.getBitmap(uri.getPath());
                            Glide.with(imageView)
                                    .asBitmap()
                                    .load(bm)
                                    .error(R.drawable.ic_baseline_choose_image_24)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL);
                        } catch (Exception e) {
                            Log.d(TAG, "onActivityResult: "+e.getLocalizedMessage());
                        }
                    }
                }
        );
    }
}