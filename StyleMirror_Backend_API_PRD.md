# 指尖 SOP / Nail-It Backend API PRD

- 版本：v1.1
- 日期：2026-06-06
- 目标：为 Nail-It 当前 MVP 定义唯一后端接口与 Supabase 数据模型，先锁定云端契约，再驱动前端实现

---

## 1. 为什么下一步先做后端契约

根据当前 [StyleMirror_Core_PRD](D:\dev\my-project\new\StyleMirror_Core_PRD.md)，产品主链路已切换为：

`链接解析 -> 款式提取 -> AI 试戴 -> BOM 清单 -> 沉浸式 SOP`

这条链里，真正决定前端页面结构和状态流的是后端返回的数据，而不是 UI 本身。

所以在开发顺序上，必须先定：

1. `Storage 上传路径`
2. `Postgres 表结构`
3. `Edge Function 路由`
4. `每个接口输入输出 JSON`
5. `异步任务状态模型`

只有这些固定后，前端页面、Room 缓存、上传任务、状态管理才不会返工。

---

## 2. 当前后端设计目标

当前阶段只服务 MVP 核心功能：

1. 短视频链接解析
2. 款式和工艺结构化提取
3. 手部照片上传
4. AI 虚拟试戴
5. BOM 清单生成
6. SOP 步骤生成

不服务：

- 登录注册
- 社区分享
- 电商交易闭环
- 多设备同步
- 完整历史系统

所以后端设计要遵循：

> **轻认证、强任务流、强结构化输出。**

---

## 3. 后端总架构

### 3.1 角色划分

#### Android 客户端负责

- 粘贴或承接链接
- 拍摄手部照片
- 上传资源
- 触发任务
- 订阅任务状态
- 承载实操卡片、计时器和免接触交互

#### Supabase 负责

- Storage 存图
- Postgres 存结构化结果
- Realtime 推送状态变化
- Edge Functions 做 AI 编排

#### AI 层负责

- 视频款式理解
- 视频工艺步骤抽取
- 手部图像试戴渲染
- BOM 清单生成
- SOP 步骤标准化输出

### 3.2 AI 编排原则

当前产品不是单一识别任务，而是一条多任务链：

1. `链接内容理解`
2. `款式结构化解析`
3. `手部图像融合 / 试戴生成`
4. `材料与工艺清单生成`
5. `SOP 步骤压缩与重写`

原则不是所有请求打一个接口，而是：

> **统一由 Edge Functions 做任务编排，对外只暴露稳定 JSON 契约。**

### 3.3 后端不只是“调模型”

后端真正负责的是一条稳定的数据管道：

1. 视频链接入库
2. 图片资产入库
3. 任务状态推进
4. 调用 AI 服务
5. 校验结构化 JSON
6. 写回 Postgres
7. 用 Realtime 驱动 Android 页面变化

所以 Edge Functions 的价值不只是封装 API key，而是成为：

> **任务编排层 + 结构化结果守门层。**

---

## 4. 身份与会话模型

当前不做登录，所以采用：

### 4.1 install_id

客户端首次启动生成：

- `install_id`

说明：

- 存 DataStore
- 作为当前设备的逻辑标识
- 所有任务和会话都挂在它下面

### 4.2 session_id

每次用户开始一个新的美甲流程时生成：

- `session_id`

说明：

- 一次链接解析 + 手部上传 + 试戴 + BOM + SOP，对应一个 session
- 一个 session 可包含多个资源和多个异步任务结果

### 4.3 约束

当前 MVP 不依赖真正身份体系，所以：

- 客户端不直接写核心表
- 核心写操作全部走 Edge Functions
- 客户端只拿 `anon key`
- 服务端用 `service role` 写库

---

## 5. Storage 设计

建议建立一个 bucket：

- `nail-it-assets`

### 5.1 路径规范

#### 链接解析中间资源

`nail-it-assets/sources/{install_id}/{session_id}/{asset_id}.json`

#### 视频关键帧或封面

`nail-it-assets/tutorials/{install_id}/{session_id}/{asset_id}.jpg`

#### 用户手部照片

`nail-it-assets/hands/{install_id}/{session_id}/{asset_id}.jpg`

#### AI 试戴结果图

`nail-it-assets/try-on/{install_id}/{session_id}/{asset_id}.jpg`

#### 实操关键帧/GIF

`nail-it-assets/sop/{install_id}/{session_id}/{asset_id}.gif`

### 5.2 客户端上传策略

当前阶段建议：

1. 客户端先调用 Edge Function 获取上传许可
2. 客户端上传到 Storage
3. 上传成功后再回调一个 confirm 接口

不建议让前端自己拼所有路径逻辑后直接写表。

---

## 6. Postgres 核心表结构

这里只定义 MVP 真正需要的表。

### 6.1 sessions

用途：

- 一个完整的教程执行流程主表

字段建议：

- `id uuid primary key`
- `install_id text not null`
- `status text not null`
- `source_type text not null`
- `source_url text`
- `style_name text`
- `created_at timestamptz default now()`
- `updated_at timestamptz default now()`

状态建议：

- `draft`
- `source_submitted`
- `source_parsing`
- `source_parsed`
- `hand_uploaded`
- `try_on_pending`
- `try_on_ready`
- `bom_ready`
- `sop_ready`
- `in_progress`
- `completed`
- `failed`

### 6.2 session_assets

用途：

- 存所有上传和生成资源的元信息

字段建议：

- `id uuid primary key`
- `session_id uuid not null references sessions(id) on delete cascade`
- `asset_type text not null`
- `storage_path text not null`
- `mime_type text`
- `sort_order int default 0`
- `created_at timestamptz default now()`

asset_type 枚举建议：

- `tutorial_frame`
- `hand_photo`
- `try_on_result`
- `sop_media`

### 6.3 source_parses

用途：

- 存视频款式和工艺的结构化解析结果

字段建议：

- `session_id uuid primary key references sessions(id) on delete cascade`
- `model text`
- `version text`
- `parse_json jsonb not null`
- `created_at timestamptz default now()`

### 6.4 try_on_results

用途：

- 存 AI 虚拟试戴结果和分析摘要

字段建议：

- `session_id uuid primary key references sessions(id) on delete cascade`
- `model text`
- `version text`
- `result_image_path text`
- `result_json jsonb not null`
- `created_at timestamptz default now()`

### 6.5 bom_lists

用途：

- 存物料清单

字段建议：

- `session_id uuid primary key references sessions(id) on delete cascade`
- `bom_json jsonb not null`
- `created_at timestamptz default now()`

### 6.6 sop_guides

用途：

- 存沉浸式实操步骤

字段建议：

- `session_id uuid primary key references sessions(id) on delete cascade`
- `version text`
- `sop_json jsonb not null`
- `created_at timestamptz default now()`

### 6.7 prompt_versions

用途：

- 记录当前 parse / try-on / bom / sop 使用的 prompt 版本

字段建议：

- `id uuid primary key`
- `prompt_type text not null`
- `version text not null`
- `description text`
- `prompt_template text`
- `created_at timestamptz default now()`

---

## 7. Realtime 订阅策略

客户端不需要订阅所有表，只订阅和当前 session 强相关的状态。

建议订阅：

1. `sessions`
   - 当前 session 状态变化
2. `source_parses`
   - 款式解析结果落库
3. `try_on_results`
   - 试戴任务完成
4. `bom_lists`
   - BOM 结果完成
5. `sop_guides`
   - SOP 结果完成

客户端监听逻辑：

- 如果 `sessions.status` 变化到 `source_parsed`，跳转解析结果页
- 如果变化到 `try_on_ready`，跳转试戴结果页
- 如果变化到 `bom_ready`，展示 BOM 清单
- 如果变化到 `sop_ready`，允许进入沉浸式实操页

---

## 8. Edge Functions 路由设计

当前阶段推荐 6 个核心函数。

### 8.1 create_session

#### 作用

创建一个新的教程执行会话。

#### 请求

```json
{
  "install_id": "string",
  "source_type": "short_video_link"
}
```

#### 响应

```json
{
  "session_id": "uuid",
  "status": "draft"
}
```

---

### 8.2 submit_source_link

#### 作用

提交短视频链接，并启动解析。

#### 请求

```json
{
  "session_id": "uuid",
  "source_url": "https://example.com/video"
}
```

#### 响应

```json
{
  "session_id": "uuid",
  "status": "source_parsing"
}
```

---

### 8.3 prepare_asset_upload

#### 作用

为当前 session 的某类资源准备上传路径或上传许可。

#### 请求

```json
{
  "session_id": "uuid",
  "asset_type": "hand_photo",
  "mime_type": "image/jpeg"
}
```

#### 响应

```json
{
  "asset_id": "uuid",
  "storage_path": "nail-it-assets/hands/{install_id}/{session_id}/{asset_id}.jpg"
}
```

备注：

- 当前 MVP 可以先返回 storage_path，由客户端直接上传
- 更严谨的版本可签发短期 upload token

---

### 8.4 confirm_asset_upload

#### 作用

告诉后端某个资源已经成功上传，并登记进 `session_assets`

#### 请求

```json
{
  "session_id": "uuid",
  "asset_id": "uuid",
  "asset_type": "hand_photo",
  "storage_path": "string"
}
```

#### 响应

```json
{
  "ok": true
}
```

---

### 8.5 create_try_on

#### 作用

根据视频解析结果 + 手部照片生成 AI 试戴结果。

#### 请求

```json
{
  "session_id": "uuid"
}
```

#### 响应

```json
{
  "session_id": "uuid",
  "status": "try_on_pending"
}
```

#### 服务端逻辑

1. 查 `source_parses`
2. 查 hand photo 资源
3. 调图像生成/编辑能力
4. 生成试戴结果图和摘要
5. 写入 `try_on_results`
6. 更新 `sessions.status = try_on_ready`

---

### 8.6 generate_execution_package

#### 作用

基于解析结果和试戴结果，生成 BOM 与 SOP。

#### 请求

```json
{
  "session_id": "uuid"
}
```

#### 响应

```json
{
  "session_id": "uuid",
  "status": "bom_ready"
}
```

#### 服务端逻辑

1. 查 `source_parses`
2. 查 `try_on_results`
3. 生成：
   - BOM 清单
   - 操作步骤
   - 倒计时建议
   - 可简化步骤
4. 写入 `bom_lists`
5. 写入 `sop_guides`
6. 更新 `sessions.status = sop_ready`

---

## 9. 结构化输出设计

必须坚持：

> 所有 AI 接口都返回结构化 JSON，不返回自由文本主结构。

当前统一策略：

- Edge Function 负责 prompt 编排、schema 校验、重试、落库
- Android 不直接依赖模型输出文本，而只依赖后端结构化结果

### 9.1 Source Parse Schema

建议包含：

- `style_name`
- `style_tags[]`
- `visual_elements[]`
- `techniques[]`
- `total_steps`
- `materials_hint[]`
- `steps[]`
  - `index`
  - `title`
  - `action`
  - `duration_hint`
  - `importance`

### 9.2 Try-On Result Schema

建议包含：

- `fit_summary`
- `tone_observation`
- `highlight_points[]`
- `risk_points[]`
- `render_variants[]`

### 9.3 BOM Schema

建议包含：

- `basic_tools[]`
- `style_specific_items[]`
- `optional_substitutes[]`
- `warnings[]`

### 9.4 SOP Schema

建议包含：

- `steps[]`
  - `index`
  - `title`
  - `instruction`
  - `media_asset_path`
  - `timer_seconds`
  - `voice_shortcut`
- `completion_tip`

---

## 10. 接口调用顺序

客户端调用顺序固定如下：

1. `create_session`
2. `submit_source_link`
3. 订阅 `source_parses`
4. `prepare_asset_upload`
5. 客户端上传手部照片
6. `confirm_asset_upload`
7. `create_try_on`
8. 订阅 `try_on_results`
9. 用户点击“开始做”
10. `generate_execution_package`
11. 订阅 `bom_lists` 与 `sop_guides`
12. 进入沉浸式实操页

---

## 11. Android 端需要配合的本地能力

即使后端先设计，客户端也要明确将来会给什么数据：

### 必须本地生成的

- `install_id`
- `session_id`

### 必须本地采集的

- 手部照片
- 可选光线校准信息
- 用户在步骤页的勾选和进度状态

### 可选后续本地生成的

- 手部轮廓辅助框
- 基础甲床区域检测
- 本地语音唤醒

当前 MVP 不需要做高复杂度 3D 手部建模，避免无意义复杂度。

---

## 12. Supabase 目录建议

建议在仓库里准备：

- `supabase/functions/create_session`
- `supabase/functions/submit_source_link`
- `supabase/functions/prepare_asset_upload`
- `supabase/functions/confirm_asset_upload`
- `supabase/functions/create_try_on`
- `supabase/functions/generate_execution_package`
- `supabase/migrations`

当前即便还没装 Supabase CLI，也应该按这个结构组织。

---

## 13. 当前最关键的后端设计判断

### 不该做的

- 直接让 Android 写核心 Postgres 表
- 用自由文本响应驱动主逻辑
- 把业务真相散落在多个页面状态里
- 把试戴结果直接作为前端临时图，不沉淀任务结果

### 应该做的

- 所有核心状态以 Postgres 为准
- 所有 AI 结果以结构化 JSON 为准
- 所有阶段推进以 `sessions.status` 为准
- Android 只做本地体验层和缓存层

---

## 14. 下一步开发优先级

现在最合理的顺序不是继续铺页面，而是：

1. 建 `supabase/` 目录
2. 写 migration 草案
3. 写 6 个 Edge Function 的 request/response 类型
4. 定 4 个 AI schema
5. 再回头让 Android 接首页、试戴和 SOP 状态流

---

## 15. 最终结论

对于 Nail-It 当前阶段来说：

> **先设计后端接口和 Supabase 数据契约，比先铺前端页面更重要。**

因为这个产品真正的复杂度不在页面，而在：

- 异步任务流
- 结构化结果
- 图片资源生命周期
- 试戴生成如何驱动决策
- SOP 如何稳定落地到前端状态流

后端契约一旦定住，前端才能稳定接入。
