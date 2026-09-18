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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.launcher3.lineage.trust.db.TrustDatabaseHelper;
import com.android.launcher3.pm.UserCache;

import java.util.HashSet;
import java.util.Set;

/**
 * Removes trust configuration that is no longer relevant.
 *
 * <p>The trust settings are keyed by package name and are never cleaned up when an app is
 * uninstalled. This reconciliation drops entries for packages that are no longer installed in any
 * user profile, so that a configuration cannot silently come back if the package name is later
 * reused. Packages that are still installed, even if they are currently not shown in the settings
 * (for example because they have no launcher icon or widgets), are kept.
 */
public final class TrustDatabaseReconciler {

    private static final String TAG = "TrustDatabaseReconciler";

    private TrustDatabaseReconciler() {
    }

    /**
     * Deletes configuration entries for packages that are not installed in any user profile.
     * Nothing is deleted if the installed packages cannot be fully enumerated.
     */
    public static void reconcile(@NonNull Context context) {
        final Context appContext = context.getApplicationContext();
        try {
            doReconcile(appContext);
        } catch (Exception e) {
            Log.w(TAG, "Failed to reconcile trust database", e);
        }
    }

    private static void doReconcile(@NonNull Context appContext) {
        final TrustDatabaseHelper dbHelper = TrustDatabaseHelper.getInstance(appContext);
        final Set<String> trackedPackages = dbHelper.getPackages();
        if (trackedPackages.isEmpty()) {
            return;
        }

        // Collect packages installed in any profile. If this fails for any profile the whole
        // reconciliation is aborted (via the caller) so we never delete based on partial data.
        final Set<String> installedPackages = new HashSet<>();
        for (UserHandle user : UserCache.getInstance(appContext).getUserProfiles()) {
            final PackageManager packageManager = appContext.createContextAsUser(user, 0)
                    .getPackageManager();
            // MATCH_UNINSTALLED_PACKAGES also returns apps that are hidden for the user or that
            // were uninstalled with their data kept. Those are not really gone, so their
            // configuration must be preserved; only fully removed packages are pruned.
            for (ApplicationInfo appInfo : packageManager.getInstalledApplications(
                    PackageManager.MATCH_UNINSTALLED_PACKAGES)) {
                installedPackages.add(appInfo.packageName);
            }
        }

        trackedPackages.removeAll(installedPackages);
        if (!trackedPackages.isEmpty()) {
            Log.i(TAG, "Removing " + trackedPackages.size() + " stale trust entries");
            dbHelper.deletePackages(trackedPackages);
        }
    }
}
