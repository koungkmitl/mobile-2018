package com.review.foodreview.component;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.review.foodreview.R;
import com.review.foodreview.dto.Review;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ReviewListAdapter extends ArrayAdapter<Review> {
    private final String TAG = "REVIEWLISTADAPTER";
    private final List<Review> reviewList;
    private final Context context;
    private StorageReference mStorageRef;

    public ReviewListAdapter(@NonNull Context context, int resource, @NonNull List<Review> reviews) {
        super(context, resource, reviews);
        Log.d(TAG, "ReviewListAdapter: New ReviewListAdapter");
        this.reviewList = reviews;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final View reviewItem = LayoutInflater.from(context).inflate(R.layout.review_item, parent, false);
        final TextView _description = reviewItem.findViewById(R.id.review_item_text_description);
        // final TextView _author = reviewItem.findViewById(R.id.review_item_text_author);
        final TextView _ratingFood = reviewItem.findViewById(R.id.review_item_rating_food);
        final TextView _ratingService = reviewItem.findViewById(R.id.review_item_rating_service);
        final TextView _ratingAtmosphere = reviewItem.findViewById(R.id.review_item_rating_atmosphere);
        final ImageView _image = reviewItem.findViewById(R.id.review_item_image_food);
        final Review review = reviewList.get(position);
        _description.setText(review.getDescription());
        _ratingFood.setText(String.valueOf(review.getRating().get("food")));
        _ratingService.setText(String.valueOf(review.getRating().get("service")));
        _ratingAtmosphere.setText(String.valueOf(review.getRating().get("atmosphere")));
        if (review.getImageUriList() != null) {
            if (review.getImageUriList().get(0) != null){
                mStorageRef = FirebaseStorage.getInstance().getReference();
                String child = review.getImageUriList().get(0);
                Log.d(TAG, child);
                StorageReference riversRef = mStorageRef.child("review/" + child);
                riversRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d(TAG, "SUCESS");
                        Log.d(TAG, String.valueOf(uri));
                        Picasso.get()
                                .load(uri)
                                .into(_image);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, e.getMessage());
                    }
                });
            }
        }
        // TODO: Get author name
        return reviewItem;
    }
}
