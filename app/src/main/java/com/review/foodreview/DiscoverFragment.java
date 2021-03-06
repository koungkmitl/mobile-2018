package com.review.foodreview;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import android.widget.SearchView;
import com.google.firebase.firestore.*;
import com.review.foodreview.component.RestaurantListItem;
import com.review.foodreview.dto.LogDTO;
import com.review.foodreview.dto.Restaurant;
import com.review.foodreview.dto.ImageModel;
import com.review.foodreview.dto.Review;
import com.review.foodreview.dto.SlidingImageAdapter;
import com.review.foodreview.sqlite.DBHelper;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DiscoverFragment extends Fragment{
    private final List<Restaurant> restaurants = new ArrayList<>();
    private static final String TAG = "DISCOVER";
    private static ViewPager mPager;
    private Fragment fragmentrestaurant;
    private DBHelper dbHelper;
    private Bundle args;
    private FirebaseFirestore mdb;
    private WormDotsIndicator wormDotsIndicator;
    private static final int NUM_PAGES = 3;
    private ArrayList<ImageModel> imageModelArrayList;
    private SearchView searchView;
    private final int[] myImageList = new int[] {
            R.drawable.slide1,
            R.drawable.slide2,
            R.drawable.slide3
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Start discover fragment (Create)");
        mdb = FirebaseFirestore.getInstance();
        args = new Bundle();
        imageModelArrayList = new ArrayList<>();
        imageModelArrayList = populateList();
        fragmentrestaurant = new RestaurantFragment();
        dbHelper = new DBHelper(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        dbHelper.createLog(new LogDTO(TAG, "Start discover fragment (CreateView)"));
        Log.d(TAG, "Start discover fragment (CreateView)");
        return inflater.inflate(R.layout.discover, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainActivity.onFragmentChanged(TAG);
        dbHelper.createLog(new LogDTO(TAG, "Start discover fragment (ActivityCreated)"));
        Log.d(TAG, "Start discover fragment (ActivityCreated)");
        // set up Featured slideshow
        Log.d(TAG, "Do setupSlideshow");
        setupSlideshow();
        // get Discover List
        getdiscoverList();
        //search
        searchbar();
    }

    //get image
    private ArrayList<ImageModel> populateList() {

        ArrayList<ImageModel> list = new ArrayList<>();
        for (int i = 0; i < NUM_PAGES; i++) {
            ImageModel imageModel = new ImageModel();
            imageModel.setImage_drawable(myImageList[i]);
            list.add(imageModel);
        }

        return list;
    }

    //setup slideshow here
    private void setupSlideshow() {
        mPager = getView().findViewById(R.id.pager);
        Log.d(TAG, String.valueOf(mPager.getCurrentItem()));
        mPager.setPageMargin(-4);
        SlidingImageAdapter slidingImageAdapter = new SlidingImageAdapter(getContext(),
                imageModelArrayList);
        slidingImageAdapter.setFragmentmanager(getActivity().getSupportFragmentManager());
        mPager.setAdapter(slidingImageAdapter);
        this.wormDotsIndicator = getView().findViewById(R.id.worm_dots_indicator);
        this.wormDotsIndicator.setViewPager(mPager);
        dbHelper.createLog(new LogDTO(TAG, "setupSlideshow: finished setup"));
        Log.d(TAG, "setupSlideshow: finished setup");
    }
    private void getdiscoverList(){
        // do with firestore
        mdb.collection("restaurant")
                .limit(8)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                restaurants.clear();
                Restaurant restaurant;
                Log.d(TAG, "Do query in Restaurant");
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    restaurant = doc.toObject(Restaurant.class);
                    restaurant.setId(doc.getId());
                    restaurants.add(restaurant);
                }
                final LinearLayout _restaurantList = getView().findViewById(R.id.discover_list);
                // add restaurant items to the LinearLayout _restaurantList
                for (final Restaurant r : restaurants) {
                    final RestaurantListItem restaurantListItem = new RestaurantListItem(getContext(), r, _restaurantList);
                    final View restaurantListItemView = restaurantListItem.getComponent();
                    _restaurantList.addView(restaurantListItemView);
                    restaurantListItemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            passbundle(r.getId(), r.getName());
                        }
                    });
                }
            }
        });
    }

    // set bundle and pass to restaurantFragment
    private void passbundle(String restaurantId, String restaurantName){
        dbHelper.createLog(new LogDTO(TAG, "Send data to RestaurantFragment"));
        Log.d(TAG, "Send data to RestaurantFragment");
        this.args.putString("restaurantId", restaurantId);
        this.args.putString("restaurantName", restaurantName);
        this.fragmentrestaurant.setArguments(args);
        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.main_view, fragmentrestaurant)
                .commit();
        Log.d(TAG, restaurantId);
    }

    private void displayDialog(String title, String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Light_Dialog_Alert);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .show();
    }
    private void searchbar(){
        searchView = getView().findViewById(R.id.discover_btn_search);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Fragment searchResultFragment = new SearchBarFragment();
                args.putString("search", query);
                searchResultFragment.setArguments(args);
                getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_view, searchResultFragment)
                        .addToBackStack(null)
                        .commit();
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }
}