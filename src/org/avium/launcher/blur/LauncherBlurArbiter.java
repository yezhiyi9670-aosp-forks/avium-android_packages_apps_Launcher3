/*
 * Copyright (C) 2026 The AviumUI Project
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
package org.avium.launcher.blur;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.view.View;

import com.android.launcher3.Launcher;

import java.util.List;

/**
 * Single arbitration point for the launcher's foreground/content blur.
 *
 * <p>Independent systems (folder open/close, launcher state transitions, the app-index overlay)
 * request a blur for their own source instead of writing a {@link RenderEffect} directly. The
 * arbiter reduces all active requests with {@code max} and is the only writer of the render effect
 * on {@link Launcher#getDepthBlurTargets()}. This keeps the sources from clobbering each other —
 * e.g. a state change clearing a blur that an open folder still owns.
 *
 * <p>Only the content channel is arbitrated here. The wallpaper/background blur continues to be
 * reduced by {@code BaseDepthController}'s depth properties, and the folder contributes to it via
 * {@code folderDepth}.
 */
public final class LauncherBlurArbiter {

    /** Owned by the folder that is currently open (or closing). */
    public static final int SOURCE_FOLDER = 0;
    /** Owned by the launcher state machinery (e.g. All Apps content blur). */
    public static final int SOURCE_STATE = 1;
    /** Owned by the app-index overlay. */
    public static final int SOURCE_OVERLAY = 2;

    private static final int SOURCE_COUNT = 3;

    private final Launcher mLauncher;
    private final float[] mSourceBlur = new float[SOURCE_COUNT];
    private float mAppliedBlur = -1f;

    public LauncherBlurArbiter(Launcher launcher) {
        mLauncher = launcher;
    }

    /**
     * Sets the blur radius requested by {@code source} and recomputes the effective blur.
     */
    public void setSourceBlur(int source, float radius) {
        if (source < 0 || source >= SOURCE_COUNT) {
            return;
        }
        float value = Math.max(0f, radius);
        if (Float.compare(mSourceBlur[source], value) == 0) {
            return;
        }
        mSourceBlur[source] = value;
        applyContentBlur();
    }

    /**
     * @return the blur radius currently requested by {@code source}.
     */
    public float getSourceBlur(int source) {
        return source >= 0 && source < SOURCE_COUNT ? mSourceBlur[source] : 0f;
    }

    private void applyContentBlur() {
        float blur = 0f;
        for (float value : mSourceBlur) {
            blur = Math.max(blur, value);
        }
        if (Float.compare(mAppliedBlur, blur) == 0) {
            return;
        }
        mAppliedBlur = blur;

        RenderEffect effect = blur > 0f
                ? RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP)
                : null;
        List<View> targets = mLauncher.getDepthBlurTargets();
        for (View target : targets) {
            if (target != null) {
                target.setRenderEffect(effect);
            }
        }
    }
}
