# FiaTimeTable

一个基于Android平台的现代化课程表应用，采用Jetpack Compose构建，提供直观的周视图和日视图，帮助用户高效管理课程安排。

## 功能特点

- 📅 **多视图模式**：支持周视图和日视图两种显示模式，满足不同场景需求
- 📚 **完整的课程管理**：支持添加、编辑、删除课程，包含丰富的课程属性
- 🎨 **自定义颜色**：为每门课程设置不同颜色，便于区分
- 📱 **现代化UI**：采用Material Design 3设计语言，界面美观易用
- 🌓 **主题切换**：支持浅色、深色和跟随系统三种主题模式
- 🔄 **学期管理**：支持多学期切换，适应不同学期的课程安排
- 💾 **数据导入导出**：支持课程数据的导入导出，方便数据迁移和备份
- 🌐 **网络课程支持**：单独管理网络选修课，不占用固定时间段
- ⏰ **灵活时间段**：可自定义每日时间段设置

## 技术栈

- **开发语言**：Kotlin
- **UI框架**：Jetpack Compose
- **架构模式**：MVVM (Model-View-ViewModel)
- **导航**：Navigation Compose
- **数据存储**：本地存储 (SharedPreferences)
- **最低支持版本**：Android 12 (API 31)
- **目标版本**：Android 15 (API 36)

## 项目结构

```
app/
├── src/main/
│   ├── java/com/glassous/fiatimetable/
│   │   ├── data/
│   │   │   ├── model/           # 数据模型
│   │   │   │   ├── Course.kt    # 课程数据模型
│   │   │   │   ├── OnlineCourse.kt  # 网络课程数据模型
│   │   │   │   ├── Term.kt      # 学期数据模型
│   │   │   │   └── TimeTableData.kt  # 课程表数据结构
│   │   │   └── repository/      # 数据仓库层
│   │   ├── navigation/
│   │   │   └── Screen.kt        # 导航屏幕定义
│   │   ├── ui/
│   │   │   ├── components/      # UI组件
│   │   │   ├── dialog/          # 对话框组件
│   │   │   ├── screen/          # 屏幕UI
│   │   │   │   ├── DayViewScreen.kt    # 日视图
│   │   │   │   ├── WeekViewScreen.kt   # 周视图
│   │   │   │   └── SettingsScreen.kt   # 设置页面
│   │   │   ├── theme/           # 主题相关
│   │   │   └── viewmodel/       # 视图模型
│   │   └── MainActivity.kt      # 主活动
│   └── res/                     # 资源文件
├── build.gradle.kts             # 应用构建配置
└── DATA_STRUCTURE.md            # 数据结构文档
```

## 数据结构

应用使用本地存储保存课程数据，主要包含以下数据集：

- **学期信息** (`xf_terms`): 存储所有学期的基本信息
- **课程数据** (`xf_courses`): 按学期、星期、时间段三级嵌套存储课程信息
- **网络选修课** (`xf_onlineCourses`): 存储不占用固定时间段的网络课程
- **时间段设置** (`xf_timeSlots`): 定义每日的时间段安排
- **当前学期** (`xf_term`): 当前选中的学期名称
- **主题设置** (`xf_theme`): 界面主题设置

详细的数据结构定义请参考 [DATA_STRUCTURE.md](DATA_STRUCTURE.md)

## 安装与运行

### 环境要求

- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 11 或更高版本
- Android SDK API 31-36
- Kotlin 2.0.21

### 构建步骤

1. 克隆项目到本地：
   ```bash
   git clone https://github.com/Glassous/FiaTimeTableAndroid.git
   cd FiaTimeTableAndroid
   ```

2. 使用Android Studio打开项目

3. 等待Gradle同步完成

4. 连接Android设备或启动模拟器

5. 点击运行按钮或使用以下命令：
   ```bash
   ./gradlew installDebug
   ```

## 使用指南

### 基本操作

1. **查看课程表**：打开应用默认显示周视图，可以查看一周的课程安排
2. **切换视图**：点击底部导航栏可以在周视图和日视图之间切换
3. **添加课程**：在周视图中点击空白时间段，填写课程信息
4. **编辑课程**：点击已有课程可以编辑课程信息
5. **切换学期**：在设置页面可以选择不同的学期

### 高级功能

1. **数据导入导出**：在设置页面可以导出当前数据为JSON文件，或导入之前备份的数据
2. **网络课程管理**：在设置页面可以添加不占用固定时间段的网络课程
3. **自定义时间段**：在设置页面可以调整每日的时间段设置
4. **主题切换**：在设置页面可以选择浅色、深色或跟随系统主题

## 开发计划

- [ ] 添加课程提醒功能
- [ ] 支持课程表小组件
- [ ] 添加课程统计功能
- [ ] 支持云同步
- [ ] 添加更多自定义选项

## 贡献指南

欢迎提交Issue和Pull Request来帮助改进项目！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 GNU General Public License v3.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

### 许可证摘要

本项目是自由软件：您可以自由地重新分发和/或修改它，但需遵守以下条款：

- **自由使用**：您可以自由地运行、研究、修改和分发本软件
- **开源要求**：如果您修改了本软件并发布，您必须以相同的GPLv3许可证发布您的修改版本
- **源码提供**：如果您分发本软件或其修改版本，您必须同时提供完整的源代码
- **禁止商用**：您不能将本软件或其修改版本用于商业目的

### 对开发者的要求

如果您基于本项目进行二次开发：

1. 您的衍生作品必须也使用GPLv3许可证
2. 您需要提供完整的源代码
3. 您需要保留原始版权声明和许可证信息
4. 您需要明确标明您对代码的修改部分

## 联系方式

如有问题或建议，请通过以下方式联系：

- 提交Issue: [GitHub Issues](https://github.com/Glassous/FiaTimeTableAndroid/issues)
- 邮箱: yongyanye614@gmail.com

## 致谢

感谢以下开源项目和库：

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代化的Android UI工具包
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) - Compose导航组件
- [Material Design 3](https://m3.material.io/) - Material Design设计系统
- [Gson](https://github.com/google/gson) - JSON序列化/反序列化库