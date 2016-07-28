package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingListItem;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.FileUtil;
import com.udacity.firebase.shoppinglistplusplus.utils.ImagePicker;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Lets user add new list item.
 */
public class AddListItemDialogFragment extends EditListDialogFragment {
    private static final int REQUEST_CODE_PICK_IMAGE = 1;
    public static String PREF = "com.udacity.firebase.shoppinglistplusplus.PREF";
    private Uri mFileUri = null;
    private static final String KEY_FILE_URI = "key_file_uri";
    DatabaseReference firebaseRef;
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedPrefEditor;
    String downloadImageUri;
    /**
     * Public static constructor that creates fragment and passes a bundle with data into it when adapter is created
     */
    public static AddListItemDialogFragment newInstance(ShoppingList shoppingList, String listId,
                                                        String encodedEmail,
                                                        HashMap<String, User> sharedWithUsers) {
        AddListItemDialogFragment addListItemDialogFragment = new AddListItemDialogFragment();
        Bundle bundle = EditListDialogFragment.newInstanceHelper(shoppingList,
                R.layout.dialog_add_item, listId, encodedEmail, sharedWithUsers);
        addListItemDialogFragment.setArguments(bundle);

        return addListItemDialogFragment;
    }

    /**
     * Initialize instance variables with data from bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPref = getActivity().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        mSharedPrefEditor = mSharedPref.edit();
        if (savedInstanceState != null) {
            mFileUri = savedInstanceState.getParcelable(KEY_FILE_URI);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable(KEY_FILE_URI, mFileUri);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        /** {@link EditListDialogFragment#createDialogHelper(int)} is a
         * superclass method that creates the dialog
         **/

        return super.createDialogHelper(R.string.positive_button_add_list_item);
    }

    /**
     * Adds new item to the current shopping list
     */

    @Override
    protected void doListEdit() {
        if (downloadUrl != null) {
             downloadImageUri = downloadUrl.toString();
        }
        String mItemName = mEditTextForList.getText().toString();
        /**
         * Adds list item if the input name is not empty
         */
        if (!mItemName.equals("")) {

            firebaseRef = FirebaseDatabase.getInstance()
                    .getReferenceFromUrl(Constants.FIREBASE_URL);
            DatabaseReference itemsRef = FirebaseDatabase.getInstance()
                    .getReferenceFromUrl(Constants.FIREBASE_URL_SHOPPING_LIST_ITEMS).child(mListId);

            /* Make a map for the item you are adding */
            HashMap<String, Object> updatedItemToAddMap = new HashMap<String, Object>();

            /* Save push() to maintain same random Id */
            DatabaseReference newRef = itemsRef.push();
            String itemId = newRef.getKey();

            /* Make a POJO for the item and immediately turn it into a HashMap */
            ShoppingListItem itemToAddObject = new ShoppingListItem(mItemName, mEncodedEmail, downloadImageUri);
            HashMap<String, Object> itemToAdd =
                    (HashMap<String, Object>) new ObjectMapper().convertValue(itemToAddObject, Map.class);


            /* Add the item to the update map*/
            updatedItemToAddMap.put("/" + Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS + "/"
                    + mListId + "/" + itemId, itemToAdd);

            /* Update affected lists timestamps */
            Utils.updateMapWithTimestampLastChanged(mSharedWith,
                    mListId, mOwner, updatedItemToAddMap);

            /* Do the update */
            firebaseRef.updateChildren(updatedItemToAddMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError firebaseError, DatabaseReference firebase) {
                    /* Now that we have the timestamp, update the reversed timestamp */
                    Utils.updateTimestampReversed(firebaseError, "AddListItem", mListId,
                            mSharedWith, mOwner);
                }
            });

            /**
             * Close the dialog fragment when done
             */
            mSharedPrefEditor.putString(Constants.KEY_FILE_URI_DOWNLOAD , null).apply();

//            AddListItemDialogFragment.this.getDialog().cancel();
        }
    }

    @Override
    protected void doPhotoUpload() {
        Intent chooseImageIntent = ImagePicker.getPickImageIntent(getActivity());
        chooseImageIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        chooseImageIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getActivity().startActivityForResult(chooseImageIntent, REQUEST_CODE_PICK_IMAGE);
        //    AddListItemDialogFragment.this.getDialog().cancel();

    }


}
