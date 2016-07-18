package com.udacity.firebase.shoppinglistplusplus.ui.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.ui.BaseActivity;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents Sign up screen and functionality of the app
 */
public class CreateAccountActivity extends BaseActivity {
    private static final String LOG_TAG = CreateAccountActivity.class.getSimpleName();
    private ProgressDialog mAuthProgressDialog;
    private DatabaseReference mFirebaseRef;
    private EditText mEditTextUsernameCreate, mEditTextEmailCreate;
    private String mUserName, mUserEmail, mPassword;
    private SecureRandom mRandom = new SecureRandom();
    public static String PREF = "com.udacity.firebase.shoppinglistplusplus.PREF";
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        /**
         * Create Firebase references
         */
        mFirebaseRef = FirebaseDatabase.getInstance()
                .getReference();

        /**
         * Link layout elements from XML and setup the progress dialog
         */
        initializeScreen();
    }

    /**
     * Override onCreateOptionsMenu to inflate nothing
     *
     * @param menu The menu with which nothing will happen
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    /**
     * Link layout elements from XML and setup the progress dialog
     */
    public void initializeScreen() {
        mEditTextUsernameCreate = (EditText) findViewById(R.id.edit_text_username_create);
        mEditTextEmailCreate = (EditText) findViewById(R.id.edit_text_email_create);
        LinearLayout linearLayoutCreateAccountActivity = (LinearLayout) findViewById(R.id.linear_layout_create_account_activity);
        initializeBackground(linearLayoutCreateAccountActivity);

        /* Setup the progress dialog that is displayed later when authenticating with Firebase */
        mAuthProgressDialog = new ProgressDialog(this);
        mAuthProgressDialog.setTitle(getResources().getString(R.string.progress_dialog_loading));
        mAuthProgressDialog.setMessage(getResources().getString(R.string.progress_dialog_check_inbox));
        mAuthProgressDialog.setCancelable(false);
    }

    /**
     * Open LoginActivity when user taps on "Sign in" textView
     */
    public void onSignInPressed(View view) {
        Intent intent = new Intent(CreateAccountActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Create new account using Firebase email/password provider
     */
    public void onCreateAccountPressed(View view) {
        mUserName = mEditTextUsernameCreate.getText().toString();
        mUserEmail = mEditTextEmailCreate.getText().toString().toLowerCase();
        mPassword = new BigInteger(130, mRandom).toString(32);

        /**
         * Check that email and user name are okay
         */
        boolean validEmail = isEmailValid(mUserEmail);
        boolean validUserName = isUserNameValid(mUserName);
        if (!validEmail || !validUserName) return;

        /**
         * If everything was valid show the progress dialog to indicate that
         * account creation has started
         */
        mAuthProgressDialog.show();

        /**
         * Create new user with specified email and password
         */
        auth.createUserWithEmailAndPassword(mUserEmail, mPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(LOG_TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(CreateAccountActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            mAuthProgressDialog.dismiss();
                        } else {
                            auth.sendPasswordResetEmail(mUserEmail)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                auth.signInWithEmailAndPassword(mUserEmail, mPassword)
                                                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                                Log.d(LOG_TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
                                                                mAuthProgressDialog.dismiss();
                                                                Log.i(LOG_TAG, getString(R.string.log_message_auth_successful));

                                                                sp = getSharedPreferences(PREF, Context.MODE_PRIVATE);
                                                                sp.edit().putString(Constants.KEY_SIGNUP_EMAIL, mUserEmail).apply();

                                                                /**
                                                                 * Save name and email to sharedPreferences to create User database record
                                                                 * when the registered user will sign in for the first time
                                                                 */


                                                                /**
                                                                 * Encode user email replacing "." with ","
                                                                 * to be able to use it as a Firebase db key
                                                                 */
                                                                auth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
                                                                                              @Override
                                                                                              public void onAuthStateChanged(@NonNull final FirebaseAuth firebaseAuth) {
                                                                                                  final FirebaseUser user = firebaseAuth.getCurrentUser();
                                                                                                  if (user != null) {
                                                                                                      sp.edit().putString(Constants.KEY_PROVIDER, user.getProviderId()).apply();
                                                                                                      createUserInFirebaseHelper((String) user.getUid());
                                                                                                  }
                                                                                              }
                                                                                          });


                                                                /**
                                                                 *  Password reset email sent, open app chooser to pick app
                                                                 *  for handling inbox email intent
                                                                 */
                                                                Intent intent = new Intent(Intent.ACTION_MAIN);
                                                                intent.addCategory(Intent.CATEGORY_APP_EMAIL);
                                                                try {
                                                                    startActivity(intent);
                                                                    finish();
                                                                } catch (android.content.ActivityNotFoundException ex) {
                                    /* User does not have any app to handle email */
                                                                }
                                                                if (!task.isSuccessful()) {
                                                                    Log.w(LOG_TAG, "signInWithEmail", task.getException());
                                                                    Toast.makeText(CreateAccountActivity.this, "Authentication failed.",
                                                                            Toast.LENGTH_SHORT).show();
                                                                }

                                                            }
                                                        });

                                            } else {
                                                Log.d(LOG_TAG, getString(R.string.log_error_failed_to_change_password));
                                                mAuthProgressDialog.dismiss();
                                            }

                                        }

                                    });

                        }


                    }
                });

    }

    /**
     * Creates a new user in Firebase from the Java POJO
     */
    private void createUserInFirebaseHelper(final String authUserId) {
        final String encodedEmail = Utils.encodeEmail(mUserEmail);
        sp.edit().putString(Constants.KEY_ENCODED_EMAIL, encodedEmail).apply();

        /**
         * Create the user and uid mapping
         */
        HashMap<String, Object> userAndUidMapping = new HashMap<String, Object>();

        /* Set raw version of date to the ServerValue.TIMESTAMP value and save into dateCreatedMap */
        HashMap<String, Object> timestampJoined = new HashMap<>();
        timestampJoined.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

        /* Create a HashMap version of the user to add */
        User newUser = new User(mUserName, encodedEmail, timestampJoined);
        HashMap<String, Object> newUserMap = (HashMap<String, Object>)
                new ObjectMapper().convertValue(newUser, Map.class);

        /* Add the user and UID to the update map */
        userAndUidMapping.put("/" + Constants.FIREBASE_LOCATION_USERS + "/" + encodedEmail,
                newUserMap);
        userAndUidMapping.put("/" + Constants.FIREBASE_LOCATION_UID_MAPPINGS + "/"
                + authUserId, encodedEmail);

        /* Try to update the database; if there is already a user, this will fail */
        mFirebaseRef.updateChildren(userAndUidMapping, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError firebaseError, DatabaseReference firebase) {
                if (firebaseError != null) {
                    /* Try just making a uid mapping */
                    mFirebaseRef.child(Constants.FIREBASE_LOCATION_UID_MAPPINGS)
                            .child(authUserId).setValue(encodedEmail);
                }
                /**
                 *  The value has been set or it failed; either way, log out the user since
                 *  they were only logged in with a temp password
                 **/
                FirebaseAuth.getInstance().signOut();
            }
        });
    }

    private boolean isEmailValid(String email) {
        boolean isGoodEmail =
                (email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches());
        if (!isGoodEmail) {
            mEditTextEmailCreate.setError(String.format(getString(R.string.error_invalid_email_not_valid),
                    email));
            return false;
        }
        return isGoodEmail;
    }

    private boolean isUserNameValid(String userName) {
        if (userName.equals("")) {
            mEditTextUsernameCreate.setError(getResources().getString(R.string.error_cannot_be_empty));
            return false;
        }
        return true;
    }


    /**
     * Show error toast to users
     */
    private void showErrorToast(String message) {
        Toast.makeText(CreateAccountActivity.this, message, Toast.LENGTH_LONG).show();
    }
}
