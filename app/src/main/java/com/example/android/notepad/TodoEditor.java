package com.example.android.notepad;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.example.android.notepad.NotePad.Todos;

import java.util.Calendar;
import java.util.Date;

public class TodoEditor extends Activity {
    private EditText mTitleText;
    private EditText mContentText;
    private DatePicker mDueDatePicker;
    private Long mRowId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todo_editor);

        // 获取控件引用 - 添加明确的类型转换（关键修改）
        mTitleText = (EditText) findViewById(R.id.todo_title);
        mContentText = (EditText) findViewById(R.id.todo_content);
        mDueDatePicker = (DatePicker) findViewById(R.id.todo_due_date);
        Button confirmButton = (Button) findViewById(R.id.confirm);

        // 初始化日期选择器为当前日期
        Calendar calendar = Calendar.getInstance();
        mDueDatePicker.init(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                null
        );

        // 检查是否是编辑模式
        mRowId = null;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mRowId = extras.getLong(Todos._ID);
            if (mRowId != null) {
                // 加载已有数据
                loadTodoData();
            }
        }

        // 确认按钮点击事件
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                saveTodo();
            }
        });
    }

    // 加载待办事项数据（新增方法）
    private void loadTodoData() {
        Uri uri = Uri.parse(Todos.CONTENT_URI + "/" + mRowId);
        String[] projection = {
                Todos.COLUMN_NAME_TITLE,
                Todos.COLUMN_NAME_CONTENT,
                Todos.COLUMN_NAME_DUE_DATE
        };

        // 查询数据
        android.database.Cursor cursor = getContentResolver().query(
                uri, projection, null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            // 填充数据到视图
            mTitleText.setText(cursor.getString(0));
            mContentText.setText(cursor.getString(1));

            // 设置日期选择器
            long dueDate = cursor.getLong(2);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(dueDate);
            mDueDatePicker.init(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    null
            );

            cursor.close();
        }
    }

    private void saveTodo() {
        String title = mTitleText.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取截止日期
        Calendar calendar = Calendar.getInstance();
        calendar.set(
                mDueDatePicker.getYear(),
                mDueDatePicker.getMonth(),
                mDueDatePicker.getDayOfMonth()
        );
        long dueDate = calendar.getTimeInMillis();

        // 准备数据
        ContentValues values = new ContentValues();
        values.put(Todos.COLUMN_NAME_TITLE, title);
        values.put(Todos.COLUMN_NAME_CONTENT, mContentText.getText().toString());
        values.put(Todos.COLUMN_NAME_DUE_DATE, dueDate);

        // 新增时设置创建日期，更新时不改变创建日期
        if (mRowId == null) {
            values.put(Todos.COLUMN_NAME_CREATE_DATE, new Date().getTime());
            values.put(Todos.COLUMN_NAME_COMPLETED, 0); // 默认未完成
        }

        // 保存数据
        if (mRowId == null) {
            // 新增
            getContentResolver().insert(Todos.CONTENT_URI, values);
            Toast.makeText(this, "待办事项已添加", Toast.LENGTH_SHORT).show();
        } else {
            // 更新
            Uri uri = Uri.parse(Todos.CONTENT_URI + "/" + mRowId);
            getContentResolver().update(uri, values, null, null);
            Toast.makeText(this, "待办事项已更新", Toast.LENGTH_SHORT).show();
        }

        finish(); // 关闭编辑界面
    }
}