package tk.talcharnes.unborify;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mindorks.placeholderview.SwipeDecor;
import com.mindorks.placeholderview.SwipePlaceHolderView;
import com.mindorks.placeholderview.listeners.ItemRemovedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import tk.talcharnes.unborify.Utilities.Constants;
import tk.talcharnes.unborify.Utilities.FirebaseConstants;
import tk.talcharnes.unborify.Utilities.Utils;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private final String LOG_TAG = MainActivityFragment.class.getSimpleName();

    FloatingActionButton fab;

    ArrayList<Photo> photoList;
    private int i = 0;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private String userId, userName;
    private String oldestPostId = "";
    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    final DatabaseReference photoReference = firebaseDatabase.getReference().child(FirebaseConstants.PHOTOS);
    DatabaseReference reportRef = firebaseDatabase.getReference().child(FirebaseConstants.REPORTS);

    //        For Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    public static final int RC_SIGN_IN = 1;
    private InterstitialAd mInterstitialAd;
    private Boolean showAd = false;
    private View rootView;
    private SwipePlaceHolderView mSwipeView;
    private Button refreshButton;
    private TextView refresh_textview;
    private Context mContext;
    private int widthInDP;
    private int heightInDP;
    private boolean refresh;
    private ArrayList<Photo> photos;

    /**
     * Constructor.
     */
    public MainActivityFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);
        photos = new ArrayList<>();
        isUserLoggedIn();

        initializeBasicSetup();

        initializeSwipePlaceHolderView();

        //initializeAd();


        return rootView;
    }

    /**
     * Checks if the user is logged in. If not, then the user is prompt to log in.
     */
    private void isUserLoggedIn() {
        //For firebase auth
        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // User is signed in
                    userId = user.getUid();
                    userName = user.getDisplayName();

                    Log.d(LOG_TAG, "onAuthStateChanged:signed_in: " + userId);


                    String token = FirebaseInstanceId.getInstance().getToken();
                    FirebaseConstants.setToken(token);
                    Log.d(LOG_TAG, "Token: " + token);

                } else {
                    // User is signed out
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder().setLogo(R.mipmap.ic_launcher)
                                    .setIsSmartLockEnabled(false)
                                    .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                    Log.d(LOG_TAG, "onAuthStateChanged:signed_out");
                }

            }
        };
    }

    /**
     * Initializes Basic stuff. The photoList, mAdView, and the fab buttons.
     */
    private void initializeBasicSetup() {
        //choose your favorite adapter
        photoList = new ArrayList<Photo>();

        //Native banner ad
        /*AdView mAdView = (AdView) rootView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/

        // Report Fab button
        refreshButton = rootView.findViewById(R.id.refreshBtn);

        refresh_textview = rootView.findViewById(R.id.refreshTitle);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                oldestPostId = "";
                refresh = false;
                refreshButton.setVisibility(View.GONE);
                refresh_textview.setVisibility(View.GONE);
                getPhotos();
            }
        });

    }

    /**
     * Initializes SwipePlaceHolderView.
     */
    private void initializeSwipePlaceHolderView() {
        mSwipeView = (SwipePlaceHolderView) rootView.findViewById(R.id.swipeView);

        int bottomMargin = Utils.dpToPx(50);
        Point windowSize = Utils.getDisplaySize(getActivity().getWindowManager());
        mSwipeView.getBuilder()
                .setDisplayViewCount(3)
                .setIsUndoEnabled(true)
                .setHeightSwipeDistFactor(10)
                .setWidthSwipeDistFactor(5)
                .setSwipeDecor(new SwipeDecor()
                        .setViewWidth((int) (windowSize.x * .99))
                        .setViewHeight(((int) (windowSize.y * .92)) - bottomMargin)
                        .setViewGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP)
                        .setPaddingTop(20)
                        .setRelativeScale(0.01f)
                        .setSwipeInMsgLayoutId(R.layout.photo_swipe_in_msg_view)
                        .setSwipeOutMsgLayoutId(R.layout.photo_swipe_out_msg_view));
        getPhotos();

        mSwipeView.addItemRemoveListener(new ItemRemovedListener() {
            @Override
            public void onItemRemoved(int count) {
                //do something when the count changes to some specific value.
                //For Example: Call server to fetch more data when count is zero
                if (count < 1) {
                    /*if (mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                    }*/
                    if(refresh) {
                        refreshButton.setVisibility(View.VISIBLE);
                        refresh_textview.setVisibility(View.VISIBLE);
                    } else {
                        getPhotos();
                    }
                }
            }
        });
    }

    /**
     * Get photos from the database and adds it to the SwipePlaceHolderView.
     */
    private void getPhotos() {
        mContext = getContext();

        final long startTime = System.currentTimeMillis();

        final boolean firstTime;
        Query query;

        final Photo adViewPhoto = new Photo();
        adViewPhoto.setAd(true);

        if(oldestPostId.isEmpty()) {
            firstTime = true;
            query = photoReference.orderByChild(Photo.URL_KEY).limitToLast(8);
        } else {
            firstTime = false;
            query = photoReference.orderByChild(Photo.URL_KEY).endAt(oldestPostId).limitToLast(8);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    int len = 0;
                    ArrayList<PhotoCard> list = new ArrayList<PhotoCard>();
                    for (DataSnapshot child : dataSnapshot.getChildren()) {

                        Photo photo = child.getValue(Photo.class);

                        assert photo != null;
                        /*final DatabaseReference photoRef = photoReference.child(PhotoUtilities
                                .removeWebPFromUrl(photo.getUrl()));*/

                        /* Randomizing votes for photos
                        photoReference.child(photo.getUrl().replace(".webp", "")).child("likes").setValue((int) (Math.random()*10));
                        photoReference.child(photo.getUrl().replace(".webp", "")).child("dislikes").setValue((int) (Math.random()*10));*/

                        list.add(new PhotoCard(mContext, photo, mSwipeView, userId, userName, photoReference,
                                reportRef));
                        if(len == 0) {
                            oldestPostId = photo.getUrl();
                            len++;
                        }
                        /* mSwipeView.addView(new PhotoCard(mContext, adViewPhoto, mSwipeView, userId,
                                photoReference, reportRef));*/
                    }

                    int stopAt = (list.size() < 8) ? -1 : 0;
                    for(int i = list.size()-1; i > stopAt; i--) {
                        mSwipeView.addView(list.get(i));
                    }
                    if(list.size() < 7) {
                        int diff = 7 - list.size();
                        while(diff > 0) {
                            mSwipeView.addView(new AdCard(mContext, mSwipeView));
                            diff--;
                        }
                        oldestPostId = "";
                        refresh = true;
                    } else {
                        mSwipeView.addView(new AdCard(mContext, mSwipeView));
                    }
                    System.out.println("Got data.");
                    mSwipeView.refreshDrawableState();
                    final long endTime = System.currentTimeMillis();
                    System.out.println("<------------------------------------> Total execution time: " + (endTime - startTime) );
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(LOG_TAG, "Failed to read value.", error.toException());
            }
        });

    }

    /**
     * Initializes Ad.
     */
    private void initializeAd() {
        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.i("Ads", "onAdLoaded");
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Log.i("Ads", "onAdFailedToLoad");
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when the ad is displayed.
                Log.i("Ads", "onAdOpened");
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
                Log.i("Ads", "onAdLeftApplication");
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the interstitial ad is closed.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
                showAd = false;
                Log.i("Ads", "onAdClosed");
            }
        });
    }

    /**
     * Adds the Auth Listener when the app is started up.
     */
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    /**
     * Removes the Auth Listener when the app is closed.
     */
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}