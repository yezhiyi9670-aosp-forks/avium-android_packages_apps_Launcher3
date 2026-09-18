/*
 * Copyright (C) 2019-2024 The LineageOS Project
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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;

import com.android.launcher3.AppFilter;
import com.android.launcher3.lineage.trust.db.TrustComponent;
import com.android.launcher3.lineage.trust.db.TrustDatabaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LoadTrustComponentsTask extends AsyncTask<Void, Integer, List<TrustComponent>> {
    @NonNull
    private TrustDatabaseHelper mDbHelper;

    @NonNull
    private PackageManager mPackageManager;

    @NonNull
    private AppWidgetManager mAppWidgetManager;

    @NonNull
    private AppFilter mAppFilter;

    @NonNull
    private Callback mCallback;

    LoadTrustComponentsTask(@NonNull TrustDatabaseHelper dbHelper,
            @NonNull PackageManager packageManager,
            @NonNull AppWidgetManager appWidgetManager,
            @NonNull AppFilter appFilter,
            @NonNull Callback callback) {
        mDbHelper = dbHelper;
        mPackageManager = packageManager;
        mAppWidgetManager = appWidgetManager;
        mAppFilter = appFilter;
        mCallback = callback;
    }

    @Override
    protected List<TrustComponent> doInBackground(Void... voids) {
        // Keyed by package name: an app is listed when it has a launcher icon or provides widgets.
        final Map<String, TrustComponent> components = new LinkedHashMap<>();

        final Intent filter = new Intent(Intent.ACTION_MAIN, null);
        filter.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = mPackageManager.queryIntentActivities(filter,
                PackageManager.GET_META_DATA);
        // Widget providers may belong to apps that have no launcher icon at all.
        List<AppWidgetProviderInfo> widgets;
        try {
            widgets = mAppWidgetManager.getInstalledProviders();
        } catch (RuntimeException e) {
            widgets = Collections.emptyList();
        }

        final int total = apps.size() + widgets.size();
        int progress = 0;

        for (ResolveInfo app : apps) {
            if (mAppFilter.shouldShowApp(app.activityInfo.getComponentName())) {
                final String pkgName = app.activityInfo.packageName;
                if (!components.containsKey(pkgName)) {
                    try {
                        final String label = mPackageManager.getApplicationLabel(
                                mPackageManager.getApplicationInfo(pkgName,
                                        PackageManager.GET_META_DATA)).toString();
                        final Drawable icon = app.loadIcon(mPackageManager);
                        components.put(pkgName, new TrustComponent(pkgName, icon, label,
                                mDbHelper.isPackageHidden(pkgName),
                                mDbHelper.isPackageProtected(pkgName)));
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }
                }
            }
            progress++;
            publishProgress(Math.round(progress * 100f / total));
        }

        for (AppWidgetProviderInfo widget : widgets) {
            if (widget.provider != null) {
                final String pkgName = widget.provider.getPackageName();
                if (!components.containsKey(pkgName)) {
                    try {
                        final ApplicationInfo appInfo = mPackageManager.getApplicationInfo(pkgName,
                                PackageManager.GET_META_DATA);
                        final Drawable icon = appInfo.loadIcon(mPackageManager);
                        components.put(pkgName, new TrustComponent(pkgName, icon,
                                mPackageManager.getApplicationLabel(appInfo).toString(),
                                mDbHelper.isPackageHidden(pkgName),
                                mDbHelper.isPackageProtected(pkgName)));
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }
                }
            }
            progress++;
            publishProgress(Math.round(progress * 100f / total));
        }

        final List<TrustComponent> list = new ArrayList<>(components.values());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Collections.sort(list, (a, b) -> {
                final boolean aAffected = a.isHidden() || a.isProtected();
                final boolean bAffected = b.isHidden() || b.isProtected();
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
    protected void onPostExecute(List<TrustComponent> trustComponents) {
        mCallback.onLoadCompleted(trustComponents);
    }

    interface Callback {
        void onLoadListProgress(int progress);
        void onLoadCompleted(List<TrustComponent> result);
    }
}
