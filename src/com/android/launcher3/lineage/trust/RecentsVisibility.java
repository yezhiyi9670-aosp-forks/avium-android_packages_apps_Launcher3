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

import androidx.annotation.NonNull;

import com.android.launcher3.lineage.trust.db.RecentsComponent;
import com.android.launcher3.lineage.trust.db.TrustDatabaseHelper;

import java.util.Collections;
import java.util.Map;

/**
 * Cached view over the per-package recents visibility configuration.
 *
 * <p>The configuration is read from {@link TrustDatabaseHelper} once and kept in memory. It is
 * reloaded automatically whenever the database changes, so consumers can query it cheaply on hot
 * paths such as task filtering and thumbnail drawing.
 */
public final class RecentsVisibility implements TrustDatabaseHelper.OnChangeListener {

    private static volatile RecentsVisibility sInstance;

    private final TrustDatabaseHelper mDbHelper;
    private volatile Map<String, Integer> mVisibilityMap = Collections.emptyMap();

    public static RecentsVisibility getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (RecentsVisibility.class) {
                if (sInstance == null) {
                    sInstance = new RecentsVisibility(context.getApplicationContext());
                }
            }
        }

        return sInstance;
    }

    private RecentsVisibility(Context context) {
        mDbHelper = TrustDatabaseHelper.getInstance(context);
        reload();
        mDbHelper.addOnChangeListener(this);
    }

    @Override
    public void onChange() {
        reload();
    }

    private synchronized void reload() {
        mVisibilityMap = mDbHelper.getRecentsVisibilityMap();
    }

    public int getVisibility(@NonNull String packageName) {
        Integer visibility = mVisibilityMap.get(packageName);
        return visibility == null ? RecentsComponent.VISIBLE : visibility;
    }

    public boolean isFullyHidden(@NonNull String packageName) {
        return getVisibility(packageName) == RecentsComponent.FULLY_HIDDEN;
    }

    public boolean isContentHidden(@NonNull String packageName) {
        return getVisibility(packageName) == RecentsComponent.CONTENT_HIDDEN;
    }

    /**
     * Whether the screenshot thumbnail of the given app must not be shown in recents. This is true
     * for both {@link RecentsComponent#CONTENT_HIDDEN} and {@link RecentsComponent#FULLY_HIDDEN}
     * apps. Note that this only affects what is drawn: the underlying snapshot is kept intact so
     * that other features (e.g. the screenshot action) still work on the real image.
     */
    public boolean shouldHideThumbnail(@NonNull String packageName) {
        return getVisibility(packageName) != RecentsComponent.VISIBLE;
    }
}
