/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.lineage.trust;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.android.launcher3.lineage.trust.db.RecentsComponent;
import com.android.launcher3.lineage.trust.db.TrustDatabaseHelper;

public class UpdateRecentsItemTask extends AsyncTask<RecentsComponent, Void, Boolean> {
    @NonNull
    private TrustDatabaseHelper mDbHelper;
    @NonNull
    private UpdateCallback mCallback;

    UpdateRecentsItemTask(@NonNull TrustDatabaseHelper dbHelper,
            @NonNull UpdateCallback callback) {
        mDbHelper = dbHelper;
        mCallback = callback;
    }

    @Override
    protected Boolean doInBackground(RecentsComponent... recentsComponents) {
        if (recentsComponents.length < 1) {
            return false;
        }

        RecentsComponent component = recentsComponents[0];
        mDbHelper.setRecentsVisibility(component.getPackageName(), component.getVisibility());
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mCallback.onUpdated(result);
    }

    interface UpdateCallback {
        void onUpdated(boolean result);
    }
}
