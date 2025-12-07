# Android NotePad 项目

这是一个基于Android平台开发的多功能记事本应用，包含笔记和待办事项两大核心功能模块。该项目是学习Android数据存储、ContentProvider模式和应用架构的优秀示例。

## 项目概述

本项目是在Android官方早期数据库操作教程基础上进行扩展和优化的完整应用，展示了Android平台下的基本数据管理和用户界面开发技术。

## 功能特性

### 📝 笔记功能
- 创建、编辑、删除和查看笔记
- 笔记标题和内容的分别编辑
- 按修改时间排序显示
- 支持分享和导出功能

### ✅ 待办事项功能
- 创建、编辑、删除待办事项
- 标记待办事项完成状态
- 设置待办事项截止日期
- 按创建时间排序显示

### 📁 实时文件夹
- 支持在主屏幕添加实时文件夹
- 快速访问笔记内容

## 技术架构

### 核心组件

| 组件名称 | 功能描述 |
|---------|---------|
| `NotePad.java` | 数据结构定义和常量声明 |
| `NotePadProvider.java` | ContentProvider实现，处理数据存储和访问 |
| `NotesList.java` | 笔记列表界面 |
| `NoteEditor.java` | 笔记编辑界面 |
| `TitleEditor.java` | 标题编辑界面 |
| `TodoList.java` | 待办事项列表界面 |
| `TodoEditor.java` | 待办事项编辑界面 |
| `NotesLiveFolder.java` | 实时文件夹实现 |

### 数据模型

#### 笔记表 (`notes`)
- `_id`: 主键ID (INTEGER)
- `title`: 笔记标题 (TEXT)
- `note`: 笔记内容 (TEXT)
- `created`: 创建时间 (INTEGER, 时间戳)
- `modified`: 修改时间 (INTEGER, 时间戳)

#### 待办事项表 (`todos`)
- `_id`: 主键ID (INTEGER)
- `title`: 待办事项标题 (TEXT)
- `content`: 待办事项内容 (TEXT)
- `completed`: 完成状态 (INTEGER, 0=未完成, 1=已完成)
- `created`: 创建时间 (INTEGER, 时间戳)
- `due_date`: 截止日期 (INTEGER, 时间戳)

### 技术特点

1. **ContentProvider模式**：采用Android推荐的ContentProvider模式管理数据访问，提供统一的数据操作接口
2. **SQLite数据库**：使用SQLite进行本地数据存储，支持数据库版本升级
3. **MVC架构**：清晰的Model-View-Controller架构设计
4. **Intent机制**：充分利用Android Intent机制实现组件间通信
5. **实时文件夹**：支持Android实时文件夹功能，增强用户体验

## 项目结构

```
NotePad1/
├── app/
│   ├── build.gradle          # 应用模块构建配置
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml  # 应用配置文件
│       │   ├── java/
│       │   │   └── com/example/android/notepad/  # 源代码目录
│       │   └── res/          # 资源文件目录
│       └── androidTest/      # 测试代码目录
├── build.gradle              # 项目构建配置
├── gradle/                   # Gradle wrapper目录
├── gradlew                   # Gradle命令脚本
├── gradlew.bat               # Windows下的Gradle命令脚本
└── settings.gradle           # 项目设置
```

## 开发环境

- Android Studio
- Android SDK
- Gradle构建工具
- Java开发环境

## 构建和运行

1. 使用Android Studio打开项目
2. 连接Android设备或启动模拟器
3. 点击"Run"按钮构建并运行应用

## 使用说明

### 笔记基本功能
1. 启动应用后默认进入笔记列表界面
2. 点击菜单中的"新建"按钮创建新笔记
3. 点击笔记条目查看或编辑内容
4. 长按笔记条目可进行删除或其他操作
5. 笔记本还有搜索检索笔记功能
6. 笔记创建记录当前时间


<img width="507" height="1143" alt="image-20251207140721984" src="https://github.com/user-attachments/assets/9dd64236-f124-45be-818f-6b3b8ae55c2b" />

<img width="519" height="1059" alt="image-20251207141531612" src="https://github.com/user-attachments/assets/f76a8591-c47d-4ba9-9c1e-e343cd2611f6" />

<img width="518" height="1146" alt="image-20251207141602021" src="https://github.com/user-attachments/assets/4170082c-99e8-486a-9461-c400bf31a5bb" />

<img width="515" height="1135" alt="image-20251207141649213" src="https://github.com/user-attachments/assets/6a287314-4d09-4f5f-b872-fbe9b67bc8f4" />


### 待办事项功能
1. 从应用菜单进入待办事项列表
2. 点击"新建"按钮创建新的待办事项
3. 点击待办事项条目进行编辑
4. 使用复选框标记待办事项完成状态

<img width="507" height="1132" alt="image-20251207141719712" src="https://github.com/user-attachments/assets/3d6cfc61-0844-4c80-bf6f-f4079f1a55aa" />

<img width="510" height="1148" alt="image-20251207141744128" src="https://github.com/user-attachments/assets/b36bf771-0b12-4412-87b6-d36ec70bc846" />

<img width="515" height="1142" alt="image-20251207141808914" src="https://github.com/user-attachments/assets/4acdeffc-14a0-4700-b942-99bc8b715cd3" />


### 笔记本云存储、备份、恢复功能
1. 点击导出笔记可将笔记本内容导入手机存储

2. 可备份笔记本内容，点击恢复笔记将使用备份内容

3. 双重保障笔记本数据安全不丢失

4. Paste粘贴功能，可用更便捷的快捷粘贴框

  <img width="511" height="1145" alt="image-20251207141857451" src="https://github.com/user-attachments/assets/62dc74f3-3359-4194-a199-0dc4265615d8" />

<img width="504" height="1121" alt="image-20251207141911153" src="https://github.com/user-attachments/assets/de68b98b-c389-4060-99f3-73e65e50d03c" />


删除两条

<img width="501" height="1146" alt="image-20251207142116028" src="https://github.com/user-attachments/assets/ca56a479-1400-4d13-bf8e-fdd499c62b32" />


成功恢复 

<img width="522" height="1137" alt="image-20251207142150478" src="https://github.com/user-attachments/assets/7ab445bd-55dc-4804-b219-e21239da0e2b" />


## 学习价值

本项目是学习Android开发的优秀示例，特别适合以下学习内容：

- Android ContentProvider的实现和使用
- SQLite数据库的创建、升级和操作
- Android UI组件的使用和界面设计
- Intent机制和组件间通信
- Android应用架构设计

## 许可证

本项目基于Apache License 2.0开源许可证。

## 参考资源

- [Android官方文档](https://developer.android.com/docs)
- [Android Sample--NotePad解析](https://blog.csdn.net/llfjfz/article/details/67638499)

