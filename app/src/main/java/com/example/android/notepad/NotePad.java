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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines a contract between the Note Pad content provider and its clients.
 * 包含笔记（Notes）和待办事项（Todos）的数据结构定义
 */
public final class NotePad {
    public static final String AUTHORITY = "com.google.provider.NotePad";

    // 禁止实例化此类
    private NotePad() {
    }

    /**
     * 笔记表数据结构定义
     */
    public static final class Notes implements BaseColumns {

        // 禁止实例化此类
        private Notes() {}

        /**
         * 表名
         */
        public static final String TABLE_NAME = "notes";

        /*
         * URI 定义
         */

        /**
         * URI 协议部分
         */
        private static final String SCHEME = "content://";

        /**
         * URI 路径部分
         */

        /**
         * 笔记列表的路径
         */
        private static final String PATH_NOTES = "/notes";

        /**
         * 单个笔记的路径（包含ID）
         */
        private static final String PATH_NOTE_ID = "/notes/";

        /**
         * 笔记ID在URI路径中的位置（0为起始索引）
         */
        public static final int NOTE_ID_PATH_POSITION = 1;

        /**
         * 实时文件夹的路径
         */
        private static final String PATH_LIVE_FOLDER = "/live_folders/notes";

        /**
         * 笔记列表的完整URI
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_NOTES);

        /**
         * 单个笔记的基础URI（需要追加ID）
         */
        public static final Uri CONTENT_ID_URI_BASE
                = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID);

        /**
         * 单个笔记的URI模式（用于匹配和构建Intent）
         */
        public static final Uri CONTENT_ID_URI_PATTERN
                = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID + "/#");

        /**
         * 实时文件夹的URI
         */
        public static final Uri LIVE_FOLDER_URI
                = Uri.parse(SCHEME + AUTHORITY + PATH_LIVE_FOLDER);

        /*
         * MIME类型定义
         */

        /**
         * 笔记列表的MIME类型
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.note";

        /**
         * 单个笔记的MIME类型
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.note";

        /**
         * 默认排序方式（按修改时间降序）
         */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        /*
         * 列定义
         */

        /**
         * 笔记标题列
         * <P>类型: TEXT</P>
         */
        public static final String COLUMN_NAME_TITLE = "title";

        /**
         * 笔记内容列
         * <P>类型: TEXT</P>
         */
        public static final String COLUMN_NAME_NOTE = "note";

        /**
         * 创建时间列
         * <P>类型: INTEGER (从System.currentTimeMillis()获取的长整数)</P>
         */
        public static final String COLUMN_NAME_CREATE_DATE = "created";

        /**
         * 修改时间列
         * <P>类型: INTEGER (从System.currentTimeMillis()获取的长整数)</P>
         */
        public static final String COLUMN_NAME_MODIFICATION_DATE = "modified";
    }

    /**
     * 新增：待办事项表数据结构定义
     */
    public static final class Todos implements BaseColumns {

        // 禁止实例化此类
        private Todos() {}

        /**
         * 表名
         */
        public static final String TABLE_NAME = "todos";

        /*
         * URI 定义
         */

        /**
         * URI 协议部分（复用笔记的协议）
         */
        private static final String SCHEME = "content://";

        /**
         * 待办事项的路径部分
         */

        /**
         * 待办事项列表的路径
         */
        private static final String PATH_TODOS = "/todos";

        /**
         * 单个待办事项的路径（包含ID）
         */
        private static final String PATH_TODO_ID = "/todos/";

        /**
         * 待办事项ID在URI路径中的位置（0为起始索引）
         */
        public static final int TODO_ID_PATH_POSITION = 1;

        /**
         * 待办事项列表的完整URI
         */
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_TODOS);

        /**
         * 单个待办事项的基础URI（需要追加ID）
         */
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME + AUTHORITY + PATH_TODO_ID);

        /**
         * 单个待办事项的URI模式
         */
        public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME + AUTHORITY + PATH_TODO_ID + "/#");

        /*
         * MIME类型定义
         */

        /**
         * 待办事项列表的MIME类型
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.todo";

        /**
         * 单个待办事项的MIME类型
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.todo";

        /**
         * 默认排序方式（按创建时间降序）
         */
        public static final String DEFAULT_SORT_ORDER = "created DESC";

        /*
         * 列定义
         */

        /**
         * 待办事项标题列
         * <P>类型: TEXT</P>
         */
        public static final String COLUMN_NAME_TITLE = "title";

        /**
         * 待办事项内容列
         * <P>类型: TEXT</P>
         */
        public static final String COLUMN_NAME_CONTENT = "content";

        /**
         * 完成状态列（0=未完成，1=已完成）
         * <P>类型: INTEGER</P>
         */
        public static final String COLUMN_NAME_COMPLETED = "completed";

        /**
         * 创建时间列
         * <P>类型: INTEGER (从System.currentTimeMillis()获取的长整数)</P>
         */
        public static final String COLUMN_NAME_CREATE_DATE = "created";

        /**
         * 截止时间列
         * <P>类型: INTEGER (从System.currentTimeMillis()获取的长整数)</P>
         */
        public static final String COLUMN_NAME_DUE_DATE = "due_date";
    }
}