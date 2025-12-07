/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.ContentProvider.PipeDataWriter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;


public class NotePadProvider extends ContentProvider implements PipeDataWriter<Cursor> {
    // Used for debugging and logging
    private static final String TAG = "NotePadProvider";

    /**
     * The database that the provider uses as its underlying data store
     */
    private static final String DATABASE_NAME = "note_pad.db";

    /**
     * 数据库版本升级为3（原版本为2），以支持待办事项表
     */
    private static final int DATABASE_VERSION = 3;

    /**
     * 投影映射：笔记表和待办事项表
     */
    private static HashMap<String, String> sNotesProjectionMap;
    private static HashMap<String, String> sTodosProjectionMap;
    private static HashMap<String, String> sLiveFolderProjectionMap;

    /**
     * 笔记查询的标准投影
     */
    private static final String[] READ_NOTE_PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_TITLE,
    };
    private static final int READ_NOTE_NOTE_INDEX = 1;
    private static final int READ_NOTE_TITLE_INDEX = 2;

    /*
     * URI匹配器的常量定义，新增待办事项相关
     */
    private static final int NOTES = 1;
    private static final int NOTE_ID = 2;
    private static final int LIVE_FOLDER_NOTES = 3;
    // 新增：待办事项列表和单个待办事项的匹配常量
    private static final int TODOS = 4;
    private static final int TODO_ID = 5;

    /**
     * URI匹配器实例
     */
    private static final UriMatcher sUriMatcher;

    // 数据库帮助类实例
    private DatabaseHelper mOpenHelper;


    /**
     * 静态代码块：初始化URI匹配器和投影映射
     */
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // 笔记相关URI
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);
        sUriMatcher.addURI(NotePad.AUTHORITY, "live_folders/notes", LIVE_FOLDER_NOTES);

        // 新增：待办事项相关URI
        sUriMatcher.addURI(NotePad.AUTHORITY, "todos", TODOS);
        sUriMatcher.addURI(NotePad.AUTHORITY, "todos/#", TODO_ID);

        /*
         * 笔记表的投影映射
         */
        sNotesProjectionMap = new HashMap<String, String>();
        sNotesProjectionMap.put(NotePad.Notes._ID, NotePad.Notes._ID);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_TITLE);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_NOTE);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE,
                NotePad.Notes.COLUMN_NAME_CREATE_DATE);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);

        /*
         * 新增：待办事项表的投影映射
         */
        sTodosProjectionMap = new HashMap<String, String>();
        sTodosProjectionMap.put(NotePad.Todos._ID, NotePad.Todos._ID);
        sTodosProjectionMap.put(NotePad.Todos.COLUMN_NAME_TITLE, NotePad.Todos.COLUMN_NAME_TITLE);
        sTodosProjectionMap.put(NotePad.Todos.COLUMN_NAME_CONTENT, NotePad.Todos.COLUMN_NAME_CONTENT);
        sTodosProjectionMap.put(NotePad.Todos.COLUMN_NAME_COMPLETED, NotePad.Todos.COLUMN_NAME_COMPLETED);
        sTodosProjectionMap.put(NotePad.Todos.COLUMN_NAME_CREATE_DATE, NotePad.Todos.COLUMN_NAME_CREATE_DATE);
        sTodosProjectionMap.put(NotePad.Todos.COLUMN_NAME_DUE_DATE, NotePad.Todos.COLUMN_NAME_DUE_DATE);

        /*
         * 实时文件夹的投影映射
         */
        sLiveFolderProjectionMap = new HashMap<String, String>();
        sLiveFolderProjectionMap.put(LiveFolders._ID, NotePad.Notes._ID + " AS " + LiveFolders._ID);
        sLiveFolderProjectionMap.put(LiveFolders.NAME, NotePad.Notes.COLUMN_NAME_TITLE + " AS " +
                LiveFolders.NAME);
    }

    /**
     * 数据库帮助类：创建和升级数据库
     */
    static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * 创建数据库表：包括笔记表和待办事项表
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            // 创建笔记表
            db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER"
                    + ");");

            // 新增：创建待办事项表
            db.execSQL("CREATE TABLE " + NotePad.Todos.TABLE_NAME + " ("
                    + NotePad.Todos._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + NotePad.Todos.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Todos.COLUMN_NAME_CONTENT + " TEXT,"
                    + NotePad.Todos.COLUMN_NAME_COMPLETED + " INTEGER DEFAULT 0,"
                    + NotePad.Todos.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Todos.COLUMN_NAME_DUE_DATE + " INTEGER"
                    + ");");
        }

        /**
         * 数据库升级：保留原有数据并添加新表
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will preserve old data");

            // 版本2升级到3时，添加待办事项表
            if (oldVersion < 3) {
                db.execSQL("CREATE TABLE " + NotePad.Todos.TABLE_NAME + " ("
                        + NotePad.Todos._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + NotePad.Todos.COLUMN_NAME_TITLE + " TEXT,"
                        + NotePad.Todos.COLUMN_NAME_CONTENT + " TEXT,"
                        + NotePad.Todos.COLUMN_NAME_COMPLETED + " INTEGER DEFAULT 0,"
                        + NotePad.Todos.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                        + NotePad.Todos.COLUMN_NAME_DUE_DATE + " INTEGER"
                        + ");");
            }
            // 如需更多版本升级，可在此添加
        }
    }

    /**
     * 初始化Provider
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    /**
     * 查询数据：支持笔记和待办事项
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String table;

        // 根据URI匹配不同的表和投影
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                qb.setTables(NotePad.Notes.TABLE_NAME);
                qb.setProjectionMap(sNotesProjectionMap);
                break;
            case NOTE_ID:
                qb.setTables(NotePad.Notes.TABLE_NAME);
                qb.setProjectionMap(sNotesProjectionMap);
                qb.appendWhere(
                        NotePad.Notes._ID + "=" +
                                uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION));
                break;
            // 新增：待办事项查询
            case TODOS:
                qb.setTables(NotePad.Todos.TABLE_NAME);
                qb.setProjectionMap(sTodosProjectionMap);
                break;
            case TODO_ID:
                qb.setTables(NotePad.Todos.TABLE_NAME);
                qb.setProjectionMap(sTodosProjectionMap);
                qb.appendWhere(
                        NotePad.Todos._ID + "=" +
                                uri.getPathSegments().get(NotePad.Todos.TODO_ID_PATH_POSITION));
                break;
            case LIVE_FOLDER_NOTES:
                qb.setTables(NotePad.Notes.TABLE_NAME);
                qb.setProjectionMap(sLiveFolderProjectionMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // 设置排序方式
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            // 根据不同表使用各自的默认排序
            if (sUriMatcher.match(uri) == TODOS || sUriMatcher.match(uri) == TODO_ID) {
                orderBy = NotePad.Todos.DEFAULT_SORT_ORDER;
            } else {
                orderBy = NotePad.Notes.DEFAULT_SORT_ORDER;
            }
        } else {
            orderBy = sortOrder;
        }

        // 执行查询
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(
                db, projection, selection, selectionArgs, null, null, orderBy);

        // 设置通知URI
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    /**
     * 获取MIME类型
     */
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NOTES:
            case LIVE_FOLDER_NOTES:
                return NotePad.Notes.CONTENT_TYPE;
            case NOTE_ID:
                return NotePad.Notes.CONTENT_ITEM_TYPE;
            // 新增：待办事项的MIME类型
            case TODOS:
                return NotePad.Todos.CONTENT_TYPE;
            case TODO_ID:
                return NotePad.Todos.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * 处理数据流（保持原有笔记功能）
     */
    static ClipDescription NOTE_STREAM_TYPES = new ClipDescription(null,
            new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN });

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        switch (sUriMatcher.match(uri)) {
            case NOTES:
            case LIVE_FOLDER_NOTES:
            case TODOS:  // 待办事项不支持数据流
                return null;
            case NOTE_ID:
                return NOTE_STREAM_TYPES.filterMimeTypes(mimeTypeFilter);
            case TODO_ID:  // 待办事项不支持数据流
                return null;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException {
        String[] mimeTypes = getStreamTypes(uri, mimeTypeFilter);
        if (mimeTypes != null) {
            Cursor c = query(
                    uri, READ_NOTE_PROJECTION, null, null, null);
            if (c == null || !c.moveToFirst()) {
                if (c != null) c.close();
                throw new FileNotFoundException("Unable to query " + uri);
            }
            return new AssetFileDescriptor(
                    openPipeHelper(uri, mimeTypes[0], opts, c, this), 0,
                    AssetFileDescriptor.UNKNOWN_LENGTH);
        }
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType,
                                Bundle opts, Cursor c) {
        FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(fout, "UTF-8"));
            pw.println(c.getString(READ_NOTE_TITLE_INDEX));
            pw.println("");
            pw.println(c.getString(READ_NOTE_NOTE_INDEX));
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Error writing data", e);
        } finally {
            c.close();
            if (pw != null) pw.flush();
            try { fout.close(); } catch (IOException e) {}
        }
    }

    /**
     * 插入数据：支持笔记和待办事项
     */
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // 验证URI
        int match = sUriMatcher.match(uri);
        if (match != NOTES && match != TODOS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();
        Long now = System.currentTimeMillis();

        // 根据不同表设置默认值
        if (match == NOTES) {
            // 笔记表默认值
            if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE)) {
                values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
            }
            if (!values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
                values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
            }
            if (!values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE)) {
                Resources r = Resources.getSystem();
                values.put(NotePad.Notes.COLUMN_NAME_TITLE, r.getString(android.R.string.untitled));
            }
            if (!values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE)) {
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
            }
        } else {
            // 新增：待办事项表默认值
            if (!values.containsKey(NotePad.Todos.COLUMN_NAME_CREATE_DATE)) {
                values.put(NotePad.Todos.COLUMN_NAME_CREATE_DATE, now);
            }
            if (!values.containsKey(NotePad.Todos.COLUMN_NAME_COMPLETED)) {
                values.put(NotePad.Todos.COLUMN_NAME_COMPLETED, 0); // 默认未完成
            }
            if (!values.containsKey(NotePad.Todos.COLUMN_NAME_TITLE)) {
                Resources r = Resources.getSystem();
                values.put(NotePad.Todos.COLUMN_NAME_TITLE, r.getString(android.R.string.untitled));
            }
            if (!values.containsKey(NotePad.Todos.COLUMN_NAME_CONTENT)) {
                values.put(NotePad.Todos.COLUMN_NAME_CONTENT, "");
            }
        }

        // 执行插入
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId;
        Uri contentUri;

        if (match == NOTES) {
            rowId = db.insert(NotePad.Notes.TABLE_NAME, NotePad.Notes.COLUMN_NAME_NOTE, values);
            contentUri = NotePad.Notes.CONTENT_ID_URI_BASE;
        } else {
            rowId = db.insert(NotePad.Todos.TABLE_NAME, NotePad.Todos.COLUMN_NAME_CONTENT, values);
            contentUri = NotePad.Todos.CONTENT_ID_URI_BASE;
        }

        if (rowId > 0) {
            Uri insertedUri = ContentUris.withAppendedId(contentUri, rowId);
            getContext().getContentResolver().notifyChange(insertedUri, null);
            return insertedUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    /**
     * 删除数据：支持笔记和待办事项
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;
        int count;
        int match = sUriMatcher.match(uri);

        switch (match) {
            case NOTES:
                count = db.delete(NotePad.Notes.TABLE_NAME, where, whereArgs);
                break;
            case NOTE_ID:
                finalWhere = NotePad.Notes._ID + "=" +
                        uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);
                if (where != null) finalWhere += " AND " + where;
                count = db.delete(NotePad.Notes.TABLE_NAME, finalWhere, whereArgs);
                break;
            // 新增：待办事项删除
            case TODOS:
                count = db.delete(NotePad.Todos.TABLE_NAME, where, whereArgs);
                break;
            case TODO_ID:
                finalWhere = NotePad.Todos._ID + "=" +
                        uri.getPathSegments().get(NotePad.Todos.TODO_ID_PATH_POSITION);
                if (where != null) finalWhere += " AND " + where;
                count = db.delete(NotePad.Todos.TABLE_NAME, finalWhere, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    /**
     * 更新数据：支持笔记和待办事项
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;
        int match = sUriMatcher.match(uri);

        // 对于笔记更新，自动更新修改时间
        if (match == NOTES || match == NOTE_ID) {
            if (values == null) {
                values = new ContentValues();
            }
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        }

        switch (match) {
            case NOTES:
                count = db.update(NotePad.Notes.TABLE_NAME, values, where, whereArgs);
                break;
            case NOTE_ID:
                String noteId = uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);
                finalWhere = NotePad.Notes._ID + "=" + noteId;
                if (where != null) finalWhere += " AND " + where;
                count = db.update(NotePad.Notes.TABLE_NAME, values, finalWhere, whereArgs);
                break;
            // 新增：待办事项更新
            case TODOS:
                count = db.update(NotePad.Todos.TABLE_NAME, values, where, whereArgs);
                break;
            case TODO_ID:
                String todoId = uri.getPathSegments().get(NotePad.Todos.TODO_ID_PATH_POSITION);
                finalWhere = NotePad.Todos._ID + "=" + todoId;
                if (where != null) finalWhere += " AND " + where;
                count = db.update(NotePad.Todos.TABLE_NAME, values, finalWhere, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    /**
     * 测试用方法
     */
    DatabaseHelper getOpenHelperForTest() {
        return mOpenHelper;
    }
}