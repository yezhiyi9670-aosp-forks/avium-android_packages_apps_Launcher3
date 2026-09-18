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

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;

import com.android.launcher3.lineage.trust.db.RecentsComponent;
import com.android.launcher3.lineage.trust.db.TrustDatabaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadRecentsComponentsTask
        extends AsyncTask<Void, Integer, List<RecentsComponent>> {
    @NonNull
    private TrustDatabaseHelper mDbHelper;

    @NonNull
    private PackageManager mPackageManager;

    @NonNull
    private Callback mCallback;

    LoadRecentsComponentsTask(@NonNull TrustDatabaseHelper dbHelper,
            @NonNull PackageManager packageManager,
            @NonNull Callback callback) {
        mDbHelper = dbHelper;
        mPackageManager = packageManager;
        mCallback = callback;
    }

    @Override
    protected List<RecentsComponent> doInBackground(Void... voids) {
        List<RecentsComponent> list = new ArrayList<>();

        // Unlike the launcher drawer, recents can contain any app that has at least one activity
        // which is able to appear in recents, whether or not it has a launcher icon.
        final List<PackageInfo> packages = mPackageManager.getInstalledPackages(
                PackageManager.GET_ACTIVITIES);

        int numPackages = packages.size();
        for (int i = 0; i < numPackages; i++) {
            PackageInfo pkg = packages.get(i);
            if (pkg.activities == null || pkg.activities.length == 0) {
                continue;
            }

            boolean hasRecentsActivity = false;
            for (ActivityInfo activity : pkg.activities) {
                if ((activity.flags & ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS) == 0) {
                    hasRecentsActivity = true;
                    break;
                }
            }
            if (!hasRecentsActivity) {
                continue;
            }

            final ApplicationInfo appInfo = pkg.applicationInfo;
            if (appInfo == null) {
                continue;
            }

            try {
                String pkgName = pkg.packageName;
                String label = mPackageManager.getApplicationLabel(appInfo).toString();
                Drawable icon = appInfo.loadIcon(mPackageManager);
                int visibility = mDbHelper.getRecentsVisibility(pkgName);

                list.add(new RecentsComponent(pkgName, icon, label, visibility));

                publishProgress(Math.round(i * 100f / numPackages));
            } catch (Exception ignored) {
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Collections.sort(list, (a, b) -> {
                final boolean aAffected = a.getVisibility() != RecentsComponent.VISIBLE;
                final boolean bAffected = b.getVisibility() != RecentsComponent.VISIBLE;
                if (aAffected != bAffected) {
                    return aAffected ? -1 : 1;
                }
                return a.getLabel().compareTo(b.getLabel());
            });
        }

        return list;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values.length > 0) {
            mCallback.onLoadListProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(List<RecentsComponent> recentsComponents) {
        mCallback.onLoadCompleted(recentsComponents);
    }

    interface Callback {
        void onLoadListProgress(int progress);
        void onLoadCompleted(List<RecentsComponent> result);
    }
}
