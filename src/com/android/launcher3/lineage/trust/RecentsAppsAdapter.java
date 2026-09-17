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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.android.launcher3.lineage.trust.db.RecentsComponent;

import java.util.ArrayList;
import java.util.List;

class RecentsAppsAdapter extends RecyclerView.Adapter<RecentsAppsAdapter.ViewHolder> {
    private static final @StringRes int[] VISIBILITY_LABELS = {
            R.string.recents_visibility_visible,
            R.string.recents_visibility_content_hidden,
            R.string.recents_visibility_fully_hidden,
    };

    private List<RecentsComponent> mList = new ArrayList<>();
    private Listener mListener;

    RecentsAppsAdapter(Listener listener) {
        mListener = listener;
    }

    public void update(List<RecentsComponent> list) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new Callback(mList, list));
        mList = list;
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recents_app, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bind(mList.get(i));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public interface Listener {
        void onRecentsItemChanged(@NonNull RecentsComponent component);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIconView;
        private final TextView mLabelView;
        private final Spinner mStateView;

        private RecentsComponent mComponent;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            mIconView = itemView.findViewById(R.id.item_recents_app_icon);
            mLabelView = itemView.findViewById(R.id.item_recents_app_title);
            mStateView = itemView.findViewById(R.id.item_recents_app_state);

            final String[] labels = new String[VISIBILITY_LABELS.length];
            for (int i = 0; i < VISIBILITY_LABELS.length; i++) {
                labels[i] = itemView.getContext().getString(VISIBILITY_LABELS[i]);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(itemView.getContext(),
                    android.R.layout.simple_spinner_item, labels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mStateView.setAdapter(adapter);
        }

        void bind(RecentsComponent component) {
            mComponent = component;

            mIconView.setImageDrawable(component.getIcon());
            mLabelView.setText(component.getLabel());

            // Set the selection without notifying, then attach the listener, so that binding
            // the current value doesn't get reported back as a user change.
            mStateView.setOnItemSelectedListener(null);
            mStateView.setSelection(component.getVisibility(), false);
            mStateView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position,
                        long id) {
                    RecentsComponent current = mComponent;
                    if (current == null || position == current.getVisibility()) {
                        return;
                    }

                    current.setVisibility(position);
                    mListener.onRecentsItemChanged(current);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    private static class Callback extends DiffUtil.Callback {
        List<RecentsComponent> mOldList;
        List<RecentsComponent> mNewList;

        public Callback(List<RecentsComponent> oldList,
                        List<RecentsComponent> newList) {
            mOldList = oldList;
            mNewList = newList;
        }

        @Override
        public int getOldListSize() {
            return mOldList.size();
        }

        @Override
        public int getNewListSize() {
            return mNewList.size();
        }

        @Override
        public boolean areItemsTheSame(int iOld, int iNew) {
            String oldPkg = mOldList.get(iOld).getPackageName();
            String newPkg = mNewList.get(iNew).getPackageName();
            return oldPkg.equals(newPkg);
        }

        @Override
        public boolean areContentsTheSame(int iOld, int iNew) {
            return mOldList.get(iOld).equals(mNewList.get(iNew));
        }
    }
}
