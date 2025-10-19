# FiaTimeTable Android

一个以 Jetpack Compose 构建的现代课程表应用，提供周视图与日视图、灵活的时间段设置、数据备份/导入，以及基于阿里云 OSS 的云端同步。项目采用 MVVM 架构，使用 SharedPreferences + Gson 持久化数据。

## 亮点功能

- 周视图与日视图：在周视图快速定位课程、支持“纯净模式”沉浸显示；日视图支持前后天切换与“明日课程预览”。
- 课程管理：添加/编辑/删除课程，支持多节连排（自动生成延续标记）。
- 学期管理与时间段：管理学期（名称、总周数、开学日期），提供“早/午/晚”分段与快速配置对话框。
- 在线课程：单独管理不占固定时间段的网络课程。
- 显示选项：可切换“显示周末”“显示休息分隔”。
- 主题与起始页：支持系统/浅色/深色主题与默认起始页（日/周）。
- 数据备份与导入：导出为 JSON，支持从备份 JSON 导入；并可一键上传/下载到阿里云 OSS。

## 架构与存储

- 技术栈：Kotlin、Jetpack Compose、Navigation Compose、MVVM。
- 数据仓库：`TimeTableRepository` 使用 `SharedPreferences` 存储，`Gson` 序列化/反序列化。
- 视图模型：`SettingsViewModel`、`WeekViewViewModel`、`DayViewViewModel`。
- 导航：`Screen` 枚举路由；`MainActivity` 初始化 `NavController` 与 `NavHost`。

## 构建环境

- 编译/目标/最低：`compileSdk=36`、`targetSdk=36`、`minSdk=31`
- Kotlin：`2.0.21`（JVM target `11`）
- Compose BOM：`2024.09.00`
- Android Gradle Plugin：`8.11.2`，Gradle：`8.13`
- 关键依赖：`androidx.compose` 系列、`navigation-compose:2.8.5`、`lifecycle-runtime-ktx:2.9.4`、`lifecycle-viewmodel-compose:2.9.4`、`gson:2.11.0`、`com.aliyun.dpa:oss-android-sdk:2.9.11`

## 权限

- 云端同步需要 `android.permission.INTERNET`（已在 `app/src/main/AndroidManifest.xml` 声明）。

## 安装与运行

- 开发环境：Android Studio（最新版推荐）、JDK 17、Android SDK API 31–36。
- 步骤：
  - 克隆仓库并用 Android Studio 打开。
  - 等待 Gradle 同步完成，连接设备或启动模拟器。
  - 运行应用或使用 `./gradlew installDebug`。

## 使用指南

- 周视图（`WeekViewScreen`）
  - 在顶部/工具栏切换周次：上一周、下一周、返回本周。
  - 点击空白格添加课程；点击已有课程进入编辑。
  - 开启“纯净模式”可隐藏系统栏，沉浸显示课程表。
  - 可在设置中切换 `显示周末` 与 `显示休息分隔`。
- 日视图（`DayViewScreen`）
  - 查看当天课程，支持前一天/后一天切换。
  - 显示“明日课程预览”，便于提前安排。
- 设置页（`SettingsScreen`）
  - 界面主题：系统/浅色/深色；起始页：日/周。
  - 学期管理：名称、总周数、开学日期（日期选择器），支持保存与切换。
  - 时间段设置：按“早/午/晚”分段添加/移除节次；快速配置可设起始时间、时长、间隔与节次数。
  - 数据备份与导入：导出课程为 JSON 文件，或从 JSON 文件导入。
  - 云端同步（OSS）：打开“OSS 配置”对话框完成配置，并在设置页上传/下载备份。

## 云端同步（阿里云 OSS）

- 依赖：`com.aliyun.dpa:oss-android-sdk:2.9.11`
- 配置项（`OssSyncConfig`）：
  - `endpoint`（必填）：例如 `https://oss-cn-hangzhou.aliyuncs.com`。
  - `bucketName`（必填）：存储桶名称。
  - `objectKey`（可选）：对象键，默认 `timetable-data.json`。
  - `accessKeyId` / `accessKeySecret`（必填）：访问凭证。
  - `regionId`（可选）：中国大陆区域选择。
- 操作：
  - 上传：将导出的备份 JSON 写入缓存后，调用 `OSSClient.putObject` 上传。
  - 下载：调用 `OSSClient.getObject` 读取备份 JSON 并解析为内部结构。
- 异常处理：`SettingsViewModel` 对 `ClientException`/`ServiceException` 做错误提示与处理。
- 安全提示：AK/SK 明文保存在本地 `SharedPreferences`，仅用于个人设备；请勿提交到公共仓库。

## 数据模型与键位

- `Course`：
  - 基本信息：`courseName`、`courseType`、`duration`、`weeksRange`、`selectedWeeks`、`color`
  - 教学信息：`teacher`、`room`、`notes`
  - 详细信息：`courseCode`、`shortName`、`courseNameEn`、`credits`、`department`、`teachingGroup`、`leader`、`courseAttr`、`assessType`、`examType`、`classroomType`、`totalHours`、`referenceBooks`、`capacity`、`enrolled`
  - 连排标记：`ContinuationMarker` 用于后续节次占位显示（continued）。
- `OnlineCourse`：`id`、`courseName`、`teacher`、`platform`、`url`、`startWeek`、`endWeek`、`credits`、`notes`
- `Term`：`name`、`weeks`、`startDate`
- `TimeTableData`：`terms`、`courses`、`onlineCourses`、`timeSlots`、`selectedTerm`、`theme`
- 持久化键（SharedPreferences）：
  - 课程表：`xf_terms`、`xf_courses`、`xf_onlineCourses`、`xf_timeSlots`、`xf_term`、`xf_theme`
  - 界面偏好：`xf_start_page`、`xf_show_weekend`、`xf_show_breaks`
  - OSS：`xf_oss_endpoint`、`xf_oss_bucket`、`xf_oss_object_key`、`xf_oss_ak_id`、`xf_oss_ak_secret`、`xf_oss_region`

## 备份与导入格式

- 导出（`exportBackupJson`）：构造包含 `terms`、`courses`（每格导出一个课程对象，忽略 continued）、`timeSlots`（对象数组：`start`/`end`/`period`）、`selectedTerm`、`theme`、`onlineCourses` 的 JSON。
- 导入（`importBackupJson`）：兼容示例结构；自动将 `duration` 生成后续节次的 continued 标记，`onlineCourses.id` 统一为字符串。

## 贡献

- 欢迎提交 Issue / Pull Request 改进项目。
- 建议在提交前运行构建与基本交互测试，确保不破坏现有功能。

## 许可证

- 本项目采用 **GNU Affero General Public License v3.0 (AGPLv3)** 许可证。
- 此许可证要求任何基于本项目的衍生作品也必须开源，包括通过网络服务提供的修改版本。
- 详细条款请参阅仓库根目录 `LICENSE` 文件与官方文本：`https://www.gnu.org/licenses/agpl-3.0.html`。

## 致谢

- Jetpack Compose / Material Design 3
- AndroidX Navigation / Lifecycle
- Gson（Google）
- 阿里云 OSS Android SDK