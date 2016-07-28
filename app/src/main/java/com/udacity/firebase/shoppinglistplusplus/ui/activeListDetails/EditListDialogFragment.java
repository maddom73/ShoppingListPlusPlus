package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.ImagePicker;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.util.HashMap;

/**
 * Base class for {@link DialogFragment}s involved with editing a shopping list.
 */
public abstract class EditListDialogFragment extends DialogFragment {
    String mListId, mOwner, mEncodedEmail;
    EditText mEditTextForList;
    Uri downloadUrl;
    int mResource;
    HashMap mSharedWith;
    ImageView mImageView;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;
    private StorageReference mStorageReferenceImages;
    private ProgressDialog mProgressDialog;
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedPrefEditor;
    public static String PREF = "com.udacity.firebase.shoppinglistplusplus.PREF";
    String imgFile;

    /**
     * Helper method that creates a basic bundle of all of the information needed to change
     * values in a shopping list.
     *
     * @param shoppingList    The shopping list that the dialog is editing
     * @param resource        The xml layout file associated with the dialog
     * @param listId          The id of the shopping list the dialog is editing
     * @param encodedEmail    The encoded email of the current user
     * @param sharedWithUsers The HashMap containing all users that the current shopping list
     *                        is shared with
     * @return The bundle containing all the arguments.
     */
    protected static Bundle newInstanceHelper(ShoppingList shoppingList, int resource, String listId,
                                              String encodedEmail, HashMap<String, User> sharedWithUsers) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.KEY_SHARED_WITH_USERS, sharedWithUsers);
        bundle.putString(Constants.KEY_LIST_ID, listId);
        bundle.putInt(Constants.KEY_LAYOUT_RESOURCE, resource);
        bundle.putString(Constants.KEY_LIST_OWNER, shoppingList.getOwner());
        bundle.putString(Constants.KEY_ENCODED_EMAIL, encodedEmail);
        return bundle;
    }

    /**
     * Initialize instance variables with data from bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedWith = (HashMap) getArguments().getSerializable(Constants.KEY_SHARED_WITH_USERS);
        mListId = getArguments().getString(Constants.KEY_LIST_ID);
        mResource = getArguments().getInt(Constants.KEY_LAYOUT_RESOURCE);
        mOwner = getArguments().getString(Constants.KEY_LIST_OWNER);
        mEncodedEmail = getArguments().getString(Constants.KEY_ENCODED_EMAIL);
        mFirebaseStorage = FirebaseStorage.getInstance();
        mSharedPref = getActivity().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        mSharedPrefEditor = mSharedPref.edit();
    }

    /**
     * Open the keyboard automatically when the dialog fragment is opened
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    protected Dialog createDialogHelper(int stringResourceForPositiveButton) {
        /* Use the Builder class for convenient dialog construction */
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.CustomTheme_Dialog);
        /* Get the layout inflater */
        LayoutInflater inflater = getActivity().getLayoutInflater();
        /* Inflate the layout, set root ViewGroup to null*/
        View rootView = inflater.inflate(mResource, null);
        mEditTextForList = (EditText) rootView.findViewById(R.id.edit_text_list_dialog);
        mImageView = (ImageView) rootView.findViewById(R.id.imagePreView);
        mImageView.setImageResource(R.drawable.placeholder_image);

        /**
         * Call doListEdit() when user taps "Done" keyboard action
         */
        mEditTextForList.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE || keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    doListEdit();

                    /**
                     * Close the dialog fragment when done
                     */
                    EditListDialogFragment.this.getDialog().cancel();
                }
                return true;
            }
        });
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPhotoUpload();
            }
        });
        /* Inflate and set the layout for the dialog */
        /* Pass null as the parent view because its going in the dialog layout */
        builder.setView(rootView)
                /* Add action buttons */
                .setPositiveButton(stringResourceForPositiveButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (Utils.getFileUri(getActivity()) != null) {
                            uploadFile(Uri.parse(Utils.getFileUri(getActivity())));
                        } else {
                            doListEdit();
                        }
                        /**
                         * Close the dialog fragment
                         */

                    }
                })
                .setNegativeButton(R.string.negative_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        /**
                         * Close the dialog fragment
                         */
                        EditListDialogFragment.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

    /**
     * Set the EditText text to be the inputted text
     * and put the pointer at the end of the input
     *
     * @param defaultText
     */
    protected void helpSetDefaultValueEditText(String defaultText) {
        mEditTextForList.setText(defaultText);
        mEditTextForList.setSelection(defaultText.length());
    }
    void uploadFile(Uri uri) {
        mStorageReference = mFirebaseStorage.getReferenceFromUrl(Constants.FIREBASE_STORAGE_URL);
        mStorageReferenceImages = mStorageReference.child("images").child(Utils.getUserID(getActivity()));
        StorageReference uploadStorageReference = mStorageReferenceImages.child(System.currentTimeMillis()+uri.getLastPathSegment());
        final UploadTask uploadTask = uploadStorageReference.putFile(uri);
        showHorizontalProgressDialog("Uploading", "Please wait...");
        uploadTask
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        hideProgressDialog();
                        downloadUrl = taskSnapshot.getDownloadUrl();
                        Log.d("MainActivity", downloadUrl.toString());
                        doListEdit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        exception.printStackTrace();
                        // Handle unsuccessful uploads
                        hideProgressDialog();
                    }
                })
                .addOnProgressListener(getActivity(), new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        int progress = (int) (100 * (float) taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        Log.i("Progress", progress + "");
                        updateProgress(progress);
                    }
                });
        mSharedPrefEditor.putString(Constants.KEY_FILE_URI, null).apply();

    }
    private void showProgressDialog(String title, String message) {
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.setMessage(message);
        else
            mProgressDialog = ProgressDialog.show(getActivity(), title, message, true, false);
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void showToast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    public void showHorizontalProgressDialog(String title, String body) {

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.setTitle(title);
            mProgressDialog.setMessage(body);
        } else {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setTitle(title);
            mProgressDialog.setMessage(body);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setProgress(0);
            mProgressDialog.setMax(100);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }
    }

    public void updateProgress(int progress) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.setProgress(progress);
        }
    }

    private void showAlertDialog(Context ctx, String title, String body, DialogInterface.OnClickListener okListener) {

        if (okListener == null) {
            okListener = new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            };
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(ctx).setMessage(body).setPositiveButton("OK", okListener).setCancelable(false);

        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }

        builder.show();
    }


    /**
     * Method to be overriden with whatever edit is supposed to happen to the list
     */
    protected abstract void doListEdit();

    protected abstract void doPhotoUpload();
}