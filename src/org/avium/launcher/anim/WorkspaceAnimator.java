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
package org.avium.launcher.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;

import com.android.launcher3.Launcher;
import com.android.launcher3.Workspace;
import com.android.launcher3.uioverrides.QuickstepLauncher;
import com.android.launcher3.statehandlers.DepthController;

import org.avium.launcher.blur.LauncherBlurArbiter;

public class WorkspaceAnimator {

    private static final float BLUR_RADIUS = 60f;
    private static final float WALLPAPER_DEPTH = 0.7f;
    private static final float WORKSPACE_SCALE = 0.92f;
    private static final int ANIM_DURATION = 200;

    private final Launcher mLauncher;
    private ValueAnimator mAnimator;

    public WorkspaceAnimator(Launcher launcher) {
        mLauncher = launcher;
    }

    public void animate(boolean show, Runnable onEnd) {
        animate(show, onEnd, true);
    }

    private void animate(boolean show, Runnable onEnd, boolean scaleWorkspace) {
        Workspace<?> workspace = mLauncher.getWorkspace();
        if (workspace == null) return;

        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }

        float targetScale = show ? WORKSPACE_SCALE : 1f;
        float startScale = workspace.getScaleX();
        float targetDepth = show ? WALLPAPER_DEPTH : 0f;

        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(ANIM_DURATION);
        mAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        mAnimator.addUpdateListener(anim -> {
            float fraction = anim.getAnimatedFraction();
            if (scaleWorkspace) {
                float scale = startScale + (targetScale - startScale) * fraction;
                workspace.setScaleX(scale);
                workspace.setScaleY(scale);
            }

            float depth = show ? (targetDepth * fraction) : (WALLPAPER_DEPTH * (1f - fraction));
            setWallpaperDepth(depth);

            float blurRadius = show ? (BLUR_RADIUS * fraction) : (BLUR_RADIUS * (1f - fraction));
            applyBlur(blurRadius);
        });

        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!show) {
                    applyBlur(0f);
                    setWallpaperDepth(0f);
                }
                if (onEnd != null) {
                    onEnd.run();
                }
            }
        });

        mAnimator.start();
    }

    public void cancel() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
    }

    private void setWallpaperDepth(float depth) {
        if (mLauncher instanceof QuickstepLauncher) {
            QuickstepLauncher quickstepLauncher = (QuickstepLauncher) mLauncher;
            DepthController depthController = quickstepLauncher.getDepthController();
            if (depthController != null) {
                depthController.stateDepth.setValue(depth);
            }
        }
    }

    private void applyBlur(float blurRadius) {
        if (mLauncher instanceof QuickstepLauncher) {
            ((QuickstepLauncher) mLauncher).getBlurArbiter()
                    .setSourceBlur(LauncherBlurArbiter.SOURCE_OVERLAY, blurRadius);
        }
    }
}
