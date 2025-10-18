# 课程表系统数据结构文档

本文档详细描述了课程表系统中所有数据的存储结构和字段定义，便于在其他平台进行开发和数据迁移。

## 数据存储概览

系统使用 localStorage 存储所有数据，主要包含以下几个数据集：

- `xf_terms`: 学期信息
- `xf_courses`: 课程数据
- `xf_onlineCourses`: 网络选修课数据
- `xf_timeSlots`: 时间段设置
- `xf_term`: 当前选中学期
- `xf_theme`: 主题设置

## 1. 学期数据结构 (Terms)

**存储键**: `xf_terms`  
**数据类型**: Array  
**描述**: 存储所有学期的基本信息

### 学期对象字段

| 字段名 | 类型 | 必填 | 默认值 | 描述 |
|--------|------|------|--------|------|
| `name` | String | ✓ | - | 学期名称，如 "2023-2024 秋季" |
| `weeks` | Number | ✓ | 16 | 学期总周数 |
| `startDate` | String | ✓ | - | 学期开始日期，格式: "YYYY-MM-DD" |

### 示例数据
```json
[
  {
    "name": "2023-2024 秋季",
    "weeks": 16,
    "startDate": "2023-09-04"
  },
  {
    "name": "2023-2024 春季",
    "weeks": 18,
    "startDate": "2024-02-26"
  }
]
```

## 2. 课程数据结构 (Courses)

**存储键**: `xf_courses`  
**数据类型**: Object  
**描述**: 按学期、星期、时间段三级嵌套存储课程信息

### 数据结构层次
```
{
  [学期名称]: {
    [星期索引 0-6]: {
      [时间段索引]: Course[] | ContinuationMarker
    }
  }
}
```

### 课程对象字段 (Course)

#### 基本信息
| 字段名 | 类型 | 必填 | 默认值 | 描述 |
|--------|------|------|--------|------|
| `courseName` | String | ✓ | - | 课程名称 |
| `courseType` | String | ✓ | "课程" | 课程类型 (课程/实验/实习/讲座等) |
| `duration` | Number | ✓ | 1 | 占用节数 (1-8) |
| `weeksRange` | String | ✓ | "1-16" | 周次范围，如 "1-16" 或 "1-8,10-16" |
| `selectedWeeks` | Array | ✓ | [] | 选中的周次数组，如 [1,2,3,4] |
| `color` | String | ✓ | - | 课程颜色，十六进制格式 "#rrggbb" |

#### 教学信息
| 字段名 | 类型 | 必填 | 默认值 | 描述 |
|--------|------|------|--------|------|
| `teacher` | String | ✗ | "" | 任课教师 |
| `room` | String | ✗ | "" | 上课地点 |
| `notes` | String | ✗ | "" | 备注信息 |

#### 详细信息
| 字段名 | 类型 | 必填 | 默认值 | 描述 |
|--------|------|------|--------|------|
| `courseCode` | String | ✗ | "" | 课程代码，如 "MATH101" |
| `shortName` | String | ✗ | "" | 课程简称 |
| `courseNameEn` | String | ✗ | "" | 英文课程名 |
| `credits` | Number | ✗ | 0 | 学分 |
| `department` | String | ✗ | "" | 开课院系 |
| `teachingGroup` | String | ✗ | "" | 教学班 |
| `leader` | String | ✗ | "" | 负责人 |
| `courseAttr` | String | ✗ | "必修" | 课程属性 (必修/选修/限选) |
| `assessType` | String | ✗ | "考试" | 考核方式 (考试/考查) |
| `examType` | String | ✗ | "闭卷" | 考试方式 (闭卷/开卷/论文) |
| `classroomType` | String | ✗ | "" | 教室类型 |
| `totalHours` | Number | ✗ | 0 | 总学时 |
| `referenceBooks` | String | ✗ | "" | 参考书目 |
| `capacity` | Number | ✗ | 0 | 容量 |
| `enrolled` | Number | ✗ | 0 | 已选人数 |

### 延续标记对象 (ContinuationMarker)

当课程占用多个时间段时，后续时间段存储延续标记：

| 字段名 | 类型 | 描述 |
|--------|------|------|
| `continued` | Boolean | 固定为 true，表示这是延续标记 |
| `fromSlot` | Number | 指向课程开始的时间段索引 |

### 示例数据
```json
{
  "2023-2024 秋季": {
    "0": {
      "0": [
        {
          "courseName": "线性代数",
          "courseType": "课程",
          "duration": 2,
          "weeksRange": "1-16",
          "selectedWeeks": [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16],
          "color": "#3b82f6",
          "teacher": "张教授",
          "room": "A101",
          "courseCode": "MATH101",
          "credits": 3,
          "courseAttr": "必修"
        }
      ],
      "1": {
        "continued": true,
        "fromSlot": 0
      }
    }
  }
}
```

## 3. 网络选修课数据结构 (OnlineCourses)

**存储键**: `xf_onlineCourses`  
**数据类型**: Object  
**描述**: 按学期存储网络选修课信息，这类课程没有固定的上课时间和地点

### 数据结构层次
```
{
  [学期名称]: OnlineCourse[]
}
```

### 网络选修课对象字段 (OnlineCourse)

#### 基本信息
| 字段名 | 类型 | 必填 | 默认值 | 描述 |
|--------|------|------|--------|------|
| `id` | String | ✓ | - | 唯一标识符，自动生成 |
| `courseName` | String | ✓ | - | 课程名称 |
| `teacher` | String | ✗ | "" | 任课教师 |
| `platform` | String | ✗ | "" | 学习平台/应用名称 |
| `url` | String | ✗ | "" | 课程网址链接 |

#### 时间信息
| 字段名 | 类型 | 必填 | 默认值 | 描述 |
|--------|------|------|--------|------|
| `startWeek` | Number | ✓ | 1 | 开始周次 |
| `endWeek` | Number | ✓ | 16 | 结束周次 |

#### 其他信息
| 字段名 | 类型 | 必填 | 默认值 | 描述 |
|--------|------|------|--------|------|
| `credits` | Number | ✗ | null | 学分 |
| `notes` | String | ✗ | "" | 备注信息 |

### 示例数据
```json
{
  "2023-2024 秋季": [
    {
      "id": "online_course_1",
      "courseName": "大学生心理健康教育",
      "teacher": "李老师",
      "platform": "学习通",
      "url": "https://mooc1.chaoxing.com/course/123456",
      "startWeek": 1,
      "endWeek": 8,
      "credits": 2,
      "notes": "需在第8周前完成所有视频学习和测试"
    },
    {
      "id": "online_course_2", 
      "courseName": "创新创业基础",
      "teacher": "王教授",
      "platform": "智慧树",
      "url": "https://www.zhihuishu.com/portals/123",
      "startWeek": 9,
      "endWeek": 16,
      "credits": 1.5,
      "notes": "包含线上学习和创业计划书撰写"
    }
  ]
}
```

## 4. 时间段设置 (TimeSlots)

**存储键**: `xf_timeSlots`  
**数据类型**: Array  
**描述**: 定义每日的时间段安排

### 数据格式
每个时间段为字符串，格式为 "HH:MM-HH:MM"

### 示例数据
```json
[
  "08:00-08:45",
  "08:55-09:40",
  "10:00-10:45",
  "10:55-11:40",
  "14:00-14:45",
  "14:55-15:40",
  "16:00-16:45",
  "16:55-17:40"
]
```

## 5. 当前学期设置 (SelectedTerm)

**存储键**: `xf_term`  
**数据类型**: String  
**描述**: 当前选中的学期名称

### 示例数据
```json
"2023-2024 秋季"
```

## 6. 主题设置 (Theme)

**存储键**: `xf_theme`  
**数据类型**: String  
**描述**: 界面主题设置

### 可选值
- `"system"`: 跟随系统
- `"light"`: 浅色主题
- `"dark"`: 深色主题

### 示例数据
```json
"system"
```

## 7. 星期索引对照表

| 索引 | 星期 |
|------|------|
| 0 | 周一 |
| 1 | 周二 |
| 2 | 周三 |
| 3 | 周四 |
| 4 | 周五 |
| 5 | 周六 |
| 6 | 周日 |

## 8. 预设颜色列表

系统提供20种预设颜色供课程选择：

```javascript
[
  '#ef4444', // red-500
  '#f97316', // orange-500
  '#f59e0b', // amber-500
  '#eab308', // yellow-500
  '#84cc16', // lime-500
  '#22c55e', // green-500
  '#10b981', // emerald-500
  '#14b8a6', // teal-500
  '#06b6d4', // cyan-500
  '#0ea5e9', // sky-500
  '#3b82f6', // blue-500
  '#6366f1', // indigo-500
  '#8b5cf6', // violet-500
  '#a855f7', // purple-500
  '#d946ef', // fuchsia-500
  '#ec4899', // pink-500
  '#f43f5e', // rose-500
  '#64748b', // slate-500
  '#6b7280', // gray-500
  '#78716c'  // stone-500
]
```

## 9. 数据导出格式

完整的数据导出应包含以下结构：

```json
{
  "version": "1.0",
  "exportDate": "2024-01-15T10:30:00.000Z",
  "data": {
    "terms": [...],
    "courses": {...},
    "onlineCourses": {...},
    "timeSlots": [...],
    "selectedTerm": "...",
    "theme": "..."
  }
}
```

## 10. 数据验证规则

### 课程数据验证
- `courseName`: 不能为空
- `duration`: 必须为 1-8 的整数
- `weeksRange`: 不能为空，格式需符合周次范围规范
- `color`: 必须为有效的十六进制颜色值
- `selectedWeeks`: 必须为数组，且与 weeksRange 对应

### 网络选修课数据验证
- `id`: 不能为空，且在同一学期内唯一
- `courseName`: 不能为空
- `startWeek`: 必须为正整数，且不大于学期总周数
- `endWeek`: 必须为正整数，且不小于 startWeek，不大于学期总周数
- `url`: 如果提供，必须为有效的URL格式
- `credits`: 如果提供，必须为正数

### 学期数据验证
- `name`: 不能为空，且在系统中唯一
- `weeks`: 必须为正整数
- `startDate`: 必须为有效的日期格式 "YYYY-MM-DD"

### 时间段数据验证
- 每个时间段必须符合 "HH:MM-HH:MM" 格式
- 开始时间必须早于结束时间
- 时间段不应重叠

---

**注意事项**:
1. 所有时间相关的数据都使用本地时区
2. 课程颜色支持自定义十六进制值
3. 多课程时间段以数组形式存储，支持同一时间段多门课程
4. 系统自动处理课程的延续标记，确保多节课正确显示
5. 周次计算基于学期开始日期的周一
6. 网络选修课独立于常规课程表存储，不占用时间段
7. 网络选修课支持设置学习平台和课程网址，便于快速访问

## 导入与导出说明

- 导出 JSON 包含键：`version`、`exportDate`、`data`。`data` 内含：`terms`、`courses`、`onlineCourses`、`timeSlots`、`selectedTerm`、`theme`。
- 导入时要求存在 `version` 与 `data`；若某些键缺失则跳过不覆盖，保持现有数据。
- 本地存储键对应：`xf_terms`、`xf_courses`、`xf_onlineCourses`、`xf_timeSlots`、`xf_term`、`xf_theme`。
- 清空数据会移除以上所有键，并重置响应式状态为默认值。
- 兼容性：当前 `version=1.0`；后续如有结构变更，应保持向后兼容或提供迁移逻辑。