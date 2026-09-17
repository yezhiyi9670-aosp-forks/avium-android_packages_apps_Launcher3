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
package com.android.launcher3.lineage.trust.db;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class RecentsComponent {
    public static final int VISIBLE = 0;
    public static final int CONTENT_HIDDEN = 1;
    public static final int FULLY_HIDDEN = 2;

    @NonNull
    private final String mPackageName;
    @NonNull
    private final Drawable mIcon;
    @NonNull
    private final String mLabel;

    private int mVisibility;

    public RecentsComponent(@NonNull String packageName, @NonNull Drawable icon,
                            @NonNull String label, int visibility) {
        mPackageName = packageName;
        mIcon = icon;
        mLabel = label;
        mVisibility = visibility;
    }

    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    @NonNull
    public Drawable getIcon() {
        return mIcon;
    }

    @NonNull
    public String getLabel() {
        return mLabel;
    }

    public int getVisibility() {
        return mVisibility;
    }

    public void setVisibility(int visibility) {
        mVisibility = visibility;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RecentsComponent)) {
            return false;
        }

        RecentsComponent otherComponent = (RecentsComponent) other;
        return otherComponent.getPackageName().equals(mPackageName) &&
                otherComponent.getVisibility() == mVisibility;
    }

    @Override
    public int hashCode() {
        return mPackageName.hashCode() + mVisibility;
    }
}
