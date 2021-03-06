/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.muzei;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.muzei.gallery.GalleryArtSource;
import com.google.android.apps.muzei.gallery.GalleryContract;
import com.google.firebase.analytics.FirebaseAnalytics;

import net.nurik.roman.muzei.R;

public class PhotoSetAsTargetActivity extends Activity {
    private static final String TAG = "PhotoSetAsTarget";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() == null || getIntent().getData() == null) {
            finish();
            return;
        }

        Uri photoUri = getIntent().getData();


        try {
            // Add the chosen photo
            ContentValues values = new ContentValues();
            values.put(GalleryContract.ChosenPhotos.COLUMN_NAME_URI, photoUri.toString());
            Uri uri = getContentResolver().insert(GalleryContract.ChosenPhotos.CONTENT_URI, values);

            // If adding the artwork succeeded, select the gallery source and publish the new image
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID,
                    new ComponentName(this, GalleryArtSource.class).flattenToShortString());
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "sources");
            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            SourceManager.selectSource(this, new ComponentName(this, GalleryArtSource.class));

            startService(new Intent(this, GalleryArtSource.class)
                    .setAction(GalleryArtSource.ACTION_PUBLISH_NEXT_GALLERY_ITEM)
                    .putExtra(GalleryArtSource.EXTRA_FORCE_URI, uri));

            // Launch main activity
            startActivity(Intent.makeMainActivity(new ComponentName(this, MuzeiActivity.class))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (SQLException e) {
            Log.e(TAG, "Unable to insert chosen artwork", e);
            Toast.makeText(this, R.string.set_as_wallpaper_failed, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
