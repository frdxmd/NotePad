package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;

public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    // 新增：待办事项菜单ID
    private static final int MENU_TODO_LIST = 100;

    /**
     * 扩展投影字段：新增修改时间列
     */
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE // 2 新增：修改时间
    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;
    /** 新增：修改时间列索引 */
    private static final int COLUMN_INDEX_MODIFY_DATE = 2;

    // 搜索相关变量
    private SimpleCursorAdapter mAdapter;
    private String mSearchKeyword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置自定义布局（包含搜索框+ListView）
        setContentView(R.layout.activity_notes_list);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        getListView().setOnCreateContextMenuListener(this);

        // 初始化适配器和搜索框
        initNotesAdapter();
        initSearchView();
    }

    // 初始化笔记列表适配器
    private void initNotesAdapter() {
        Cursor cursor = getNotesCursor(mSearchKeyword);

        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
        };
        int[] viewIDs = {
                android.R.id.text1,
                R.id.note_modify_time
        };

        mAdapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                cursor,
                dataColumns,
                viewIDs,
                0
        ) {
            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                super.bindView(view, context, cursor);

                // 强制类型转换 View -> TextView
                TextView timeView = (TextView) view.findViewById(R.id.note_modify_time);

                // 格式化时间戳
                long modifyTime = cursor.getLong(COLUMN_INDEX_MODIFY_DATE);
                String timeStr = DateFormat.format("yyyy-MM-dd HH:mm", new Date(modifyTime)).toString();

                timeView.setText(timeStr);
            }
        };

        setListAdapter(mAdapter);
    }

    // 初始化搜索框
    private void initSearchView() {
        // 强制类型转换 View -> SearchView
        SearchView searchView = (SearchView) findViewById(R.id.note_search_view);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    mSearchKeyword = query.trim();
                    updateNotesList();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    mSearchKeyword = newText.trim();
                    updateNotesList();
                    return true;
                }
            });

            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    mSearchKeyword = "";
                    updateNotesList();
                    return false;
                }
            });
        }
    }

    // 根据搜索关键词获取笔记游标
    private Cursor getNotesCursor(String keyword) {
        String selection = null;
        String[] selectionArgs = null;

        if (!TextUtils.isEmpty(keyword)) {
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                    NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
            selectionArgs = new String[]{"%" + keyword + "%", "%" + keyword + "%"};
        }

        return managedQuery(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );
    }

    // 更新笔记列表
    private void updateNotesList() {
        Cursor oldCursor = mAdapter.getCursor();
        if (oldCursor != null && !oldCursor.isClosed()) {
            oldCursor.close();
        }

        Cursor newCursor = getNotesCursor(mSearchKeyword);
        mAdapter.changeCursor(newCursor);
    }

    // ========== 导出/备份/恢复核心功能 ==========
    // 1. 导出笔记为本地TXT文件
    private void exportNotes() {
        try {
            // 获取所有笔记数据
            Cursor cursor = managedQuery(NotePad.Notes.CONTENT_URI,
                    new String[]{NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_NOTE},
                    null, null, null);

            // 拼接笔记内容
            StringBuilder sb = new StringBuilder();
            if (cursor.moveToFirst()) {
                do {
                    String title = cursor.getString(cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE));
                    String content = cursor.getString(cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE));
                    sb.append("========== 笔记：").append(title == null ? "无标题" : title).append(" ==========\n")
                            .append(content == null ? "" : content).append("\n\n");
                } while (cursor.moveToNext());
            }
            cursor.close();

            // 使用应用私有目录（无需WRITE_EXTERNAL_STORAGE权限）
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (!dir.exists()) {
                dir.mkdirs(); // 创建目录
            }
            File file = new File(dir, "notes_export.txt");
            FileWriter writer = new FileWriter(file);
            writer.write(sb.toString());
            writer.close();

            Toast.makeText(this, "导出成功！路径：" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // 2. 备份笔记（序列化到SharedPreferences）
    private void backupNotes() {
        try {
            // 获取所有笔记数据并转JSON
            Cursor cursor = managedQuery(NotePad.Notes.CONTENT_URI,
                    new String[]{NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE},
                    null, null, null);

            JSONArray jsonArray = new JSONArray();
            if (cursor.moveToFirst()) {
                do {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("id", cursor.getLong(0));
                    jsonObj.put("title", cursor.getString(1) == null ? "" : cursor.getString(1));
                    jsonObj.put("content", cursor.getString(2) == null ? "" : cursor.getString(2));
                    jsonObj.put("time", cursor.getLong(3));
                    jsonArray.put(jsonObj);
                } while (cursor.moveToNext());
            }
            cursor.close();

            // 保存到SharedPreferences
            getSharedPreferences("NotesBackup", Context.MODE_PRIVATE)
                    .edit()
                    .putString("backup_data", jsonArray.toString())
                    .apply();

            Toast.makeText(this, "备份成功！（已保存到本地）", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "备份失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // 3. 恢复笔记（从SharedPreferences反序列化）
    private void restoreNotes() {
        try {
            // 读取备份数据
            String backupData = getSharedPreferences("NotesBackup", Context.MODE_PRIVATE)
                    .getString("backup_data", "");
            if (backupData.isEmpty()) {
                Toast.makeText(this, "无备份数据！", Toast.LENGTH_SHORT).show();
                return;
            }

            // 解析JSON
            JSONArray jsonArray = new JSONArray(backupData);
            ContentValues values = new ContentValues();

            // 遍历恢复每条笔记
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);

                // 检查笔记是否已存在
                Cursor cursor = getContentResolver().query(
                        NotePad.Notes.CONTENT_URI,
                        new String[]{NotePad.Notes._ID},
                        NotePad.Notes._ID + "=?",
                        new String[]{String.valueOf(jsonObj.getLong("id"))},
                        null);

                if (cursor == null || cursor.getCount() == 0) {
                    // 新增笔记
                    values.clear();
                    values.put(NotePad.Notes.COLUMN_NAME_TITLE, jsonObj.getString("title"));
                    values.put(NotePad.Notes.COLUMN_NAME_NOTE, jsonObj.getString("content"));
                    values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, jsonObj.getLong("time"));
                    getContentResolver().insert(NotePad.Notes.CONTENT_URI, values);
                }
                if (cursor != null) cursor.close();
            }

            // 刷新列表
            updateNotesList();
            Toast.makeText(this, "恢复成功！共恢复 " + jsonArray.length() + " 条笔记", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "恢复失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // 权限申请回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportNotes();
            } else {
                Toast.makeText(this, "需要文件读写权限才能导出笔记！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ========== 菜单和事件处理 ==========
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // 修改：将图标替换为更通用的ic_menu_add
        menu.add(Menu.NONE, MENU_TODO_LIST, Menu.NONE, "待办事项")
                .setIcon(android.R.drawable.ic_menu_add);

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(this, NoteEditor.class));
        intent.setData(getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        if (clipboard != null && clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            mPasteItem.setEnabled(false);
        }

        final boolean haveItems = getListAdapter().getCount() > 0;

        if (haveItems) {
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());
            Intent[] specifics = new Intent[1];

            specifics[0] = new Intent(this, NoteEditor.class);
            specifics[0].setAction(Intent.ACTION_EDIT);
            specifics[0].setData(uri);

            MenuItem[] items = new MenuItem[1];
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(this, NoteEditor.class));
            intent.setData(uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,
                    Menu.NONE,
                    Menu.NONE,
                    null,
                    specifics,
                    intent,
                    Menu.NONE,
                    items
            );
            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        // 新增：处理待办事项入口点击
        if (itemId == MENU_TODO_LIST) {
            Intent intent = new Intent(this, TodoList.class);
            startActivity(intent);
            return true;
        }
        else if (itemId == R.id.menu_add) {
            Intent insertIntent = new Intent(this, NoteEditor.class);
            insertIntent.setAction(Intent.ACTION_INSERT);
            insertIntent.setData(getIntent().getData());
            startActivity(insertIntent);
            return true;
        } else if (itemId == R.id.menu_paste) {
            Intent pasteIntent = new Intent(this, NoteEditor.class);
            pasteIntent.setAction(Intent.ACTION_PASTE);
            pasteIntent.setData(getIntent().getData());
            startActivity(pasteIntent);
            return true;
        }
        // 导出笔记
        else if (itemId == R.id.menu_export) {
            exportNotes();
            return true;
        }
        // 备份笔记
        else if (itemId == R.id.menu_backup) {
            backupNotes();
            return true;
        }
        // 恢复笔记
        else if (itemId == R.id.menu_restore) {
            restoreNotes();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        Intent intent = new Intent(this, NoteEditor.class);
        intent.setData(Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id)));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        int id = item.getItemId();
        if (id == R.id.context_open) {
            Intent editIntent = new Intent(this, NoteEditor.class);
            editIntent.setAction(Intent.ACTION_EDIT);
            editIntent.setData(noteUri);
            startActivity(editIntent);
            return true;
        } else if (id == R.id.context_copy) {
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newUri(
                        getContentResolver(),
                        "Note",
                        noteUri));
            }
            return true;
        } else if (id == R.id.context_delete) {
            getContentResolver().delete(
                    noteUri,
                    null,
                    null
            );
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        String action = getIntent().getAction();

        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            Intent editIntent = new Intent(this, NoteEditor.class);
            editIntent.setAction(Intent.ACTION_EDIT);
            editIntent.setData(uri);
            startActivity(editIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Cursor cursor = mAdapter.getCursor();
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }
}