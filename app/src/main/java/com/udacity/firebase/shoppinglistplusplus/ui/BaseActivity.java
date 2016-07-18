package com.udacity.firebase.shoppinglistplusplus.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.ui.login.CreateAccountActivity;
import com.udacity.firebase.shoppinglistplusplus.ui.login.LoginActivity;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

/**
 * BaseActivity class is used as a base class for all activities in the app
 * It implements GoogleApiClient callbacks to enable "Logout" in all activities
 * and defines variables that are being shared across all activities
 */
public abstract class BaseActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {
    protected String mProvider, mEncodedEmail;
    /* Client used to interact with Google APIs. */
    protected GoogleApiClient mGoogleApiClient;
    protected FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    protected DatabaseReference mFirebaseRef;
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedPrefEditor;
    public static String PREF = "com.udacity.firebase.shoppinglistplusplus.PREF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Setup the Google API object to allow Google logins */

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        /**
         * Build a GoogleApiClient with access to the Google Sign-In API and the
         * options specified by gso.
         */

        /* Setup the Google API object to allow Google+ logins */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        auth = FirebaseAuth.getInstance();
        /**
         * Getting mProvider and mEncodedEmail from SharedPreferences
         */

        /* Get mEncodedEmail and mProvider from SharedPreferences, use null as default value */

        mProvider = Utils.getProvider(this);
        mEncodedEmail = Utils.getEncodedMail(this);


        if (!((this instanceof LoginActivity) || (this instanceof CreateAccountActivity))) {
            mFirebaseRef = FirebaseDatabase.getInstance()
                    .getReferenceFromUrl(Constants.FIREBASE_URL);
            mAuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull final FirebaseAuth firebaseAuth) {
                    final FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {

                        Log.i("AuthStateChanged", "User is signed in with uid: " + user.getUid());
                    } else {
                        Log.i("AuthStateChanged", "No user is signed in.");
                        mSharedPref = getSharedPreferences(PREF, Context.MODE_PRIVATE);
                        mSharedPrefEditor = mSharedPref.edit();
                        mSharedPrefEditor.putString(Constants.KEY_ENCODED_EMAIL, null);
                        mSharedPrefEditor.putString(Constants.KEY_PROVIDER, null);

                        takeUserToLoginScreenOnUnAuth();
                    }
                }
            };

        }
    }



    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Inflate the menu; this adds items to the action bar if it is present. */
        getMenuInflater().inflate(R.menu.menu_base, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }

        if (id == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void initializeBackground(LinearLayout linearLayout) {

        /**
         * Set different background image for landscape and portrait layouts
         */
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            linearLayout.setBackgroundResource(R.drawable.background_loginscreen_land);
        } else {
            linearLayout.setBackgroundResource(R.drawable.background_loginscreen);
        }
    }

    /**
     * Logs out the user from their current session and starts LoginActivity.
     * Also disconnects the mGoogleApiClient if connected and provider is Google
     */
    protected void logout() {

        /* Logout if mProvider is not null */
        if (mProvider != null) {
            FirebaseAuth.getInstance().signOut();

            if (mProvider.equals(Constants.GOOGLE_PROVIDER)) {

                /* Logout from Google+ */
                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                //nothing
                            }
                        });
            }
        }
        takeUserToLoginScreenOnUnAuth();
    }

    private void takeUserToLoginScreenOnUnAuth() {
        /* Move user to LoginActivity, and remove the backstack */
        Intent intent = new Intent(BaseActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

}
