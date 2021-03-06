package com.review.foodreview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static android.app.Activity.RESULT_OK;

public class ReviewEditFragment extends Fragment {

    private static final int RESULT_LOAD_IAMGE = 1;
    private static final String TAG = "REVIEWEDIT";

    private Toolbar _toolbar;
    private EditText _reviewText;
    private RatingBar _ratingBarFood, _ratingBarService, _ratingBarAtmosphere;
    private Button _btnUploadPhoto;
    private ImageView _imageFood;
    private ProgressBar _loading;

    private Map<String, Object> data, rating;

    private Intent galleryImage;

    private Uri selectImage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.review_edit, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "load REVIEWEDIT fragment");
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        createMenu();
        onClickUploadPhoto();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "Click submit");

        // Init FirebaseStorage and FirebaseStore
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageReference = firebaseStorage.getReference();
        final StorageReference storage = storageReference.child("review");

        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Init FirebaseAuth
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        // get value from .xml
        _reviewText = getView().findViewById(R.id.review_edit_description);
        _ratingBarFood = getView().findViewById(R.id.review_edit_star_food);
        _ratingBarService = getView().findViewById(R.id.review_edit_star_service);
        _ratingBarAtmosphere = getView().findViewById(R.id.review_edit_star_atmosphere);
        _loading = getView().findViewById(R.id.review_edit_loading);

        // FirebaseAuth uid
        String uid = firebaseAuth.getUid();

        // convert value
        String stringReview = _reviewText.getText().toString();
        float floatFood = _ratingBarFood.getRating();
        float floatService = _ratingBarService.getRating();
        float floatAtmosphere = _ratingBarAtmosphere.getRating();

        // get parameter from previous fragment using bundle
        Bundle bundle = getArguments();

        if (validateForm(uid, bundle)) {
            Log.d(TAG, "Some fields were empty");
            Toast
                    .makeText(getActivity(), "Some fields were empty", Toast.LENGTH_SHORT)
                    .show();
        } else {
            _loading.setVisibility(View.VISIBLE);
            final String restaurantId = bundle.getString("restaurantId");
            final String imageName = UUID.randomUUID().toString();

            DocumentReference userRef = db.collection("user").document(uid);
            DocumentReference restaurantRef = db.collection("restaurant").document(restaurantId);

            rating = new HashMap<>();
            rating.put("atmosphere", floatAtmosphere);
            rating.put("food", floatFood);
            rating.put("service", floatService);

            data = new HashMap<>();
            data.put("author", userRef);
            data.put("restaurant", restaurantRef);
            data.put("date", new Date());
            data.put("description", stringReview);
            data.put("rating", rating);
            data.put("imageUri", Arrays.asList(imageName));

            db.collection("review").add(data).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    Log.d(TAG, "Add data to firestore success");

                    StorageReference review = storage.child(imageName);

                    Bitmap bitmap = ((BitmapDrawable) _imageFood.getDrawable()).getBitmap();
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();

                    review.putBytes(byteArray).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Log.d(TAG, "firebase storage success");
                            _loading.setVisibility(View.INVISIBLE);
                            Toast
                                    .makeText(getActivity(), "Review added!", Toast.LENGTH_SHORT)
                                    .show();
                            getActivity()
                                    .getSupportFragmentManager()
                                    .beginTransaction()
                                    .addToBackStack(null)
                                    .replace(R.id.main_view, new DiscoverFragment())
                                    .commit();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            _loading.setVisibility(View.INVISIBLE);
                            Log.d(TAG, "firebase storage failure");
                            Toast
                                    .makeText(getActivity(), "Can't add review. Something went wrong.", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    _loading.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "Add data to firestore failure");
                    Toast
                            .makeText(getActivity(), "Can't add review. Something went wrong.", Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }

        return super.onOptionsItemSelected(item);
    }

    private void createMenu() {
        _toolbar = getActivity().findViewById(R.id.review_edit_toolbar);
        _toolbar.setTitle("Write a review");
        _toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        _toolbar.inflateMenu(R.menu.review_edit);
        getActivity().setActionBar(_toolbar);
        _toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().onBackPressed();
            }
        });
    }

    private void onClickUploadPhoto() {
        Log.d(TAG, "Click btn upload image");
        _imageFood = getView().findViewById(R.id.review_edit_show_photo_upload);
        _btnUploadPhoto = getView().findViewById(R.id.register_edit_insert_photo);

        _btnUploadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                galleryImage = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryImage, RESULT_LOAD_IAMGE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IAMGE && resultCode == RESULT_OK && data != null) {
            selectImage = data.getData();

            _imageFood.setImageURI(selectImage);
        }
    }

    private boolean validateForm(String uid, Bundle bundle) {
        String text1 = _reviewText.getText().toString();
        float float1 = _ratingBarFood.getRating();
        float float2 = _ratingBarAtmosphere.getRating();
        float float3 = _ratingBarService.getRating();

        if (_imageFood.getDrawable() == null) {
            return true;
        } else if (text1.isEmpty() || float1 < 1.0 || float2 < 1.0 || float3 < 1.0) {
            return true;
        } else if (uid == null || bundle == null) {
            return true;
        }
        return false;
    }
}
