package com.example.android.notepad;

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.notepad.NotePad.Todos;

public class TodoList extends ListActivity {
    private static final String[] PROJECTION = new String[] {
            Todos._ID,
            Todos.COLUMN_NAME_TITLE,
            Todos.COLUMN_NAME_COMPLETED,
            Todos.COLUMN_NAME_DUE_DATE
    };

    private static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_COMPLETED = 2;

    // 定义菜单ID常量
    private static final int MENU_ADD_TODO = 1;
    private static final int MENU_CLEAR_COMPLETED = 2;
    private static final int MENU_BACK_TO_NOTES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todo_list);

        // 添加返回按钮点击事件，使用正确的类型转换
        View backButtonView = findViewById(R.id.back_to_notes_button);
        if (backButtonView instanceof Button) {
            Button backButton = (Button) backButtonView;
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); // 返回笔记列表
                }
            });
        }

        getListView().setOnCreateContextMenuListener(this);
        fillData();
    }

    private void fillData() {
        Cursor cursor = getContentResolver().query(
                Todos.CONTENT_URI,
                PROJECTION,
                null,
                null,
                Todos.DEFAULT_SORT_ORDER
        );
        startManagingCursor(cursor);
        TodoAdapter adapter = new TodoAdapter(cursor);
        setListAdapter(adapter);
    }

    // 内部类形式的适配器
    private class TodoAdapter extends CursorAdapter {
        public TodoAdapter(Cursor c) {
            super(TodoList.this, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.todo_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, final Cursor cursor) {
            // 安全地获取和转换视图
            View titleView = view.findViewById(R.id.todo_title);
            View checkBoxView = view.findViewById(R.id.todo_checkbox);

            if (titleView instanceof TextView && checkBoxView instanceof CheckBox) {
                TextView todoTitle = (TextView) titleView;
                CheckBox todoCheckbox = (CheckBox) checkBoxView;

                // 获取待办事项ID和标题
                final long id = cursor.getLong(COLUMN_INDEX_ID);
                String title = cursor.getString(COLUMN_INDEX_TITLE);
                int completed = cursor.getInt(COLUMN_INDEX_COMPLETED);

                todoTitle.setText(title);
                todoCheckbox.setChecked(completed == 1);

                // 存储ID到视图标签
                view.setTag(id);

                // 设置勾选框状态变化监听器
                todoCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        updateTodoStatus(id, isChecked);
                    }
                });
            }
        }
    }

    // 更新待办事项状态的方法
    private void updateTodoStatus(long id, boolean isCompleted) {
        ContentValues values = new ContentValues();
        values.put(Todos.COLUMN_NAME_COMPLETED, isCompleted ? 1 : 0);

        Uri uri = Uri.parse(Todos.CONTENT_URI + "/" + id);
        getContentResolver().update(uri, values, null, null);
        fillData(); // 刷新列表
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_BACK_TO_NOTES, Menu.NONE, "返回笔记")
                .setIcon(android.R.drawable.ic_menu_revert);
        menu.add(Menu.NONE, MENU_ADD_TODO, Menu.NONE, "添加待办")
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(Menu.NONE, MENU_CLEAR_COMPLETED, Menu.NONE, "清除已完成")
                .setIcon(android.R.drawable.ic_menu_delete);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_BACK_TO_NOTES:
                finish();
                return true;
            case MENU_ADD_TODO:
                Intent intent = new Intent(this, TodoEditor.class);
                startActivity(intent);
                return true;
            case MENU_CLEAR_COMPLETED:
                getContentResolver().delete(
                        Todos.CONTENT_URI,
                        Todos.COLUMN_NAME_COMPLETED + " = 1",
                        null
                );
                fillData();
                Toast.makeText(this, "已清除完成的事项", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, TodoEditor.class);
        Uri uri = Uri.parse(Todos.CONTENT_URI + "/" + id);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fillData();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // 使用try-catch避免菜单文件不存在的错误
        try {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.todo_context_menu, menu);
        } catch (Exception e) {
            // 如果菜单文件不存在，至少添加基本的删除选项
            menu.add(Menu.NONE, 100, Menu.NONE, "删除");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // 简单实现删除功能
        if (item.getItemId() == 100) {
            // 这里可以添加删除逻辑
            Toast.makeText(this, "删除功能可以在这里实现", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onContextItemSelected(item);
    }
}