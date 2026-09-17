/*
 * Copyright (C) 2019 The LineageOS Project
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class TrustDatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "trust_apps_db";

    private static final String TABLE_NAME = "trust_apps";
    private static final String KEY_UID = "uid";
    private static final String KEY_PKGNAME = "pkgname";
    private static final String KEY_HIDDEN = "hidden";
    private static final String KEY_PROTECTED = "protected";
    private static final String KEY_RECENTS = "recents";

    @Nullable
    private static TrustDatabaseHelper sSingleton;

    private final List<OnChangeListener> mChangeListeners = new CopyOnWriteArrayList<>();
    private volatile int mRecentsVisibilityVersion = 0;

    private TrustDatabaseHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized TrustDatabaseHelper getInstance(@NonNull Context context) {
        if (sSingleton == null) {
            sSingleton = new TrustDatabaseHelper(context);
        }

        return sSingleton;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CMD_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME +
                "(" +
                KEY_UID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_PKGNAME + " TEXT," +
                KEY_HIDDEN + " INTEGER DEFAULT 0," +
                KEY_PROTECTED + " INTEGER DEFAULT 0," +
                KEY_RECENTS + " INTEGER DEFAULT 0" +
                ")";
        db.execSQL(CMD_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_RECENTS
                    + " INTEGER DEFAULT 0");
        }
    }

    public void addHiddenApp(@NonNull String packageName) {
        if (isPackageHidden(packageName)) {
            return;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(KEY_PKGNAME, packageName);
            values.put(KEY_HIDDEN, 1);

            int rows = db.update(TABLE_NAME, values, KEY_PKGNAME + " = ?",
                    new String[]{KEY_PKGNAME});
            if (rows != 1) {
                // Entry doesn't exist, create a new one
                db.insertOrThrow(TABLE_NAME, null, values);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Ignored
        } finally {
            db.endTransaction();
        }
    }

    public void addProtectedApp(@NonNull String packageName) {
        if (isPackageProtected(packageName)) {
            return;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(KEY_PKGNAME, packageName);
            values.put(KEY_PROTECTED, 1);

            int rows = db.update(TABLE_NAME, values, KEY_PKGNAME + " = ?",
                    new String[]{KEY_PKGNAME});
            if (rows != 1) {
                // Entry doesn't exist, create a new one
                db.insertOrThrow(TABLE_NAME, null, values);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Ignored
        } finally {
            db.endTransaction();
        }
    }


    public void removeHiddenApp(@NonNull String packageName) {
        if (!isPackageHidden(packageName)) {
            return;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(KEY_HIDDEN, 0);

            db.update(TABLE_NAME, values, KEY_PKGNAME + " = ?", new String[]{packageName});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Ignored
        } finally {
            db.endTransaction();
        }
    }

    public void removeProtectedApp(@NonNull String packageName) {
        if (!isPackageProtected(packageName)) {
            return;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(KEY_PROTECTED, 0);

            db.update(TABLE_NAME, values, KEY_PKGNAME + " = ?", new String[]{packageName});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Ignored
        } finally {
            db.endTransaction();
        }
    }

    public boolean isPackageHidden(@NonNull String packageName) {
        String query = String.format("SELECT * FROM %s WHERE %s = ? AND %s = ?", TABLE_NAME,
                KEY_PKGNAME, KEY_HIDDEN);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{packageName, String.valueOf(1)});
        boolean result = false;
        try {
            result = cursor.getCount() != 0;
        } catch (Exception e) {
            // Ignored
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return result;
    }

    public boolean isPackageProtected(@NonNull String packageName) {
        String query = String.format("SELECT * FROM %s WHERE %s = ? AND %s = ?", TABLE_NAME,
                KEY_PKGNAME, KEY_PROTECTED);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{packageName, String.valueOf(1)});
        boolean result = false;
        try {
            result = cursor.getCount() != 0;
        } catch (Exception e) {
            // Ignored
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return result;
    }

    public int getRecentsVisibility(@NonNull String packageName) {
        String query = String.format("SELECT %s FROM %s WHERE %s = ?", KEY_RECENTS, TABLE_NAME,
                KEY_PKGNAME);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{packageName});
        int result = RecentsComponent.VISIBLE;
        try {
            if (cursor.moveToFirst()) {
                result = cursor.getInt(0);
            }
        } catch (Exception e) {
            // Ignored
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return result;
    }

    public void setRecentsVisibility(@NonNull String packageName, int visibility) {
        if (visibility == getRecentsVisibility(packageName)) {
            return;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(KEY_RECENTS, visibility);

            int rows = db.update(TABLE_NAME, values, KEY_PKGNAME + " = ?",
                    new String[]{packageName});
            if (rows == 0) {
                // Entry doesn't exist, create a new one
                values.put(KEY_PKGNAME, packageName);
                db.insertOrThrow(TABLE_NAME, null, values);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Ignored
        } finally {
            db.endTransaction();
        }

        mRecentsVisibilityVersion++;
        notifyChanged();
    }

    public int getRecentsVisibilityVersion() {
        return mRecentsVisibilityVersion;
    }

    /**
     * Returns a map of package name to the configured recents visibility for every app that is not
     * {@link RecentsComponent#VISIBLE}.
     */
    @NonNull
    public Map<String, Integer> getRecentsVisibilityMap() {
        Map<String, Integer> result = new HashMap<>();
        String query = String.format("SELECT %s, %s FROM %s WHERE %s != 0", KEY_PKGNAME, KEY_RECENTS,
                TABLE_NAME, KEY_RECENTS);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        try {
            while (cursor.moveToNext()) {
                result.put(cursor.getString(0), cursor.getInt(1));
            }
        } catch (Exception e) {
            // Ignored
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return result;
    }

    public void addOnChangeListener(@NonNull OnChangeListener listener) {
        mChangeListeners.add(listener);
    }

    public void removeOnChangeListener(@NonNull OnChangeListener listener) {
        mChangeListeners.remove(listener);
    }

    private void notifyChanged() {
        for (OnChangeListener listener : mChangeListeners) {
            listener.onChange();
        }
    }

    public interface OnChangeListener {
        void onChange();
    }
}
