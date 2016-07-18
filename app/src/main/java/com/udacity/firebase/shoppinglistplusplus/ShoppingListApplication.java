package com.udacity.firebase.shoppinglistplusplus;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;

/**
 * Includes one-time initialization of Firebase related code
 */
public class ShoppingListApplication extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        /* Enable disk persistence  */
        if (!FirebaseApp.getApps(this).isEmpty()) {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);
        }

    }

}