# 指尖 SOP / Nail-It

`Nail-It` 是一个 Android 优先的美甲 AI 产品原型，目标是把短视频美甲教程压缩成一条可执行的主链路：

`看教程 -> AI 试戴 -> 准备材料 -> 跟着做 -> 生成分享海报`

当前仓库已经不再是早期的纯骨架状态，而是一个带有 Android 客户端、Supabase Edge Functions、Qwen/OpenAI 模型编排和基础分享页的可推进 Demo。

## 项目定位

这个项目聚焦的不是泛美妆，而是美甲场景里最核心的三个问题：

1. 用户刷到短视频教程，但不知道这个款式上自己手上会不会翻车。
2. 用户知道喜欢这个款，但不会从视频里稳定提取出材料和步骤。
3. 用户开始做以后，双手不方便操作屏幕，需要更轻量、更沉浸的带做体验。

所以项目的主任务很明确：

1. 解析教程风格
2. 基于手图生成试戴结果
3. 输出 BOM 和 SOP
4. 承接后续分享

## 当前实现状态

### Android 端

当前 Android 客户端基于 Kotlin + Jetpack Compose，已经有一套可串联的页面流：

- `首页 Home`
- `灵感 Inspiration`
- `我的 Profile`
- `试戴 Adaptation`
- `准备开始 / BOM`
- `带做 Conversation`
- `完成拍摄 FinishCapture`
- `分享海报 SharePoster`

其中：

- 首页已经改成更接近 `demo-final` 的轻量化结构
- 已有底部三 tab：`首页 / 灵感 / 我的`
- 当前核心链路仍然保留：`home -> adaptation -> prepare -> conversation`
- UI 上仍有部分历史命名或未完全统一的区域，后续会继续收敛

### 后端 / 云函数

仓库内已经包含完整的 Supabase Edge Functions 目录，不是空壳：

- `create_session`
- `submit_source_link`
- `prepare_asset_upload`
- `confirm_asset_upload`
- `create_try_on`
- `render_try_on`
- `generate_execution_package`
- `create_qwen_temp_token`
- `main`

这些函数已经承担实际链路中的职责：

- 创建会话
- 写入 session 状态
- 处理手图/模板图上传
- 发起试戴任务
- 生成试戴结果
- 生成 BOM 和 SOP
- 为实时语音/视频链路预留临时 token 能力

## 当前模型链路

项目当前采用的是分工式模型架构，而不是单模型包办：

### 1. 教程解析 / 结构化输出

主要由 Qwen 负责：

- 文本与多模态解析走 `QWEN_TEXT_MODEL / QWEN_VL_MODEL`
- 用于解析教程风格、结构化输出步骤、生成执行包

对应实现主要在：

- [qwen.ts](/D:/dev/my-project/new/supabase/functions/_shared/qwen.ts)
- [submit_source_link/index.ts](/D:/dev/my-project/new/supabase/functions/submit_source_link/index.ts)
- [generate_execution_package/index.ts](/D:/dev/my-project/new/supabase/functions/generate_execution_package/index.ts)

### 2. 试戴图生成

试戴生图链路当前是：

1. 读取用户手图
2. 读取模板图或教程首图
3. 先由 Qwen 做视觉分析与 prompt 规划
4. 再由 OpenAI 图片模型生成试戴结果

默认图片模型配置为：

- `OPENAI_IMAGE_MODEL = gpt-image-2`

对应实现主要在：

- [openai.ts](/D:/dev/my-project/new/supabase/functions/_shared/openai.ts)
- [render_try_on/index.ts](/D:/dev/my-project/new/supabase/functions/render_try_on/index.ts)

### 3. 实时带做能力

当前仓库保留了实时链路的后端入口：

- `create_qwen_temp_token`

它的用途是为后续实时音视频或语音会话生成临时 token，但这部分体验仍在持续收敛中，现阶段更适合把它理解成“后续能力入口”，而不是完全完成的最终态。

## 技术栈

### 客户端

- Kotlin
- Jetpack Compose
- Navigation Compose
- Coroutines
- Hilt
- Camera / 图片选择相关能力

### 服务端

- Supabase
- Supabase Postgres
- Supabase Storage
- Supabase Edge Functions
- Deno / TypeScript

### 模型

- Qwen 多模态/文本模型
- OpenAI / 兼容 OpenAI 接口的图片模型

## 仓库结构

```text
.
├── app/                         Android 客户端
├── supabase/                    Supabase 配置与 Edge Functions
├── inspiration/                 灵感参考图
├── videos/                      视频/帧素材
├── README.md
├── StyleMirror_Core_PRD.md
├── StyleMirror_Backend_API_PRD.md
├── deploy_functions.sh
└── deploy_server.sh
```

## Android 本地开发

### 1. 配置 Android SDK

确保本地 `local.properties` 存在，例如：

```properties
sdk.dir=C\:\\Users\\37453\\AppData\\Local\\Android\\Sdk
```

### 2. 常用命令

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
./gradlew ktlintCheck
```

### 3. 当前已验证

最近一次本地验证已经通过：

```bash
./gradlew.bat assembleDebug
```

## Supabase Functions 重新部署

如果你是 Docker 挂载方式部署 Edge Functions，可以直接用这句：

```bash
cd ~/nait && git pull && rm -rf /root/supabase-docker/docker/volumes/functions/* && cp -a ~/nait/supabase/functions/. /root/supabase-docker/docker/volumes/functions/ && cd /root/supabase-docker/docker && docker compose up -d --force-recreate --no-deps functions && docker logs -f --tail 100 supabase-edge-functions
```

如果只是单纯重建函数容器：

```bash
cd /root/supabase-docker/docker && docker compose up -d --force-recreate --no-deps functions
```

## 当前产品主链路

以现在仓库里的实现为准，主链路是：

1. 首页输入短视频链接或直接选择模板款式
2. 进入试戴页
3. 上传或拍摄手图
4. 发起试戴任务并等待结果
5. 进入 BOM 清单页
6. 进入带做页
7. 最终生成分享海报

这条链路已经基本具备前后端骨架和主要状态流转，但仍有一些正在持续优化的问题：

- 试戴效果稳定性还不够强
- 视觉风格统一度还在收敛
- 实时带做体验还不是最终态
- 后端异步任务与超时处理还需要进一步工程化

## 环境变量

常见环境变量包括：

### OpenAI / 图片生成

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_MODEL`
- `OPENAI_IMAGE_MODEL`

### Qwen / DashScope

- `QWEN_API_KEY`
- `QWEN_BASE_URL`
- `QWEN_TEXT_MODEL`
- `QWEN_VL_MODEL`
- `DASHSCOPE_API_KEY`
- `DASHSCOPE_REGION`
- `DASHSCOPE_REALTIME_MODEL`

## 文档说明

- [StyleMirror_Core_PRD.md](/D:/dev/my-project/new/StyleMirror_Core_PRD.md)
  当前产品主 PRD，承接“指尖 SOP / Nail-It”的产品定义。
- [StyleMirror_Backend_API_PRD.md](/D:/dev/my-project/new/StyleMirror_Backend_API_PRD.md)
  当前后端接口与 Edge Functions 设计文档。
- [supabase/README.md](/D:/dev/my-project/new/supabase/README.md)
  后端部署与函数链路说明。

## 现阶段一句话总结

这不是一个空白脚手架，而是一个已经跑出 `试戴 + BOM + SOP + 分享` 主链路雏形的美甲 AI Android 项目，当前重点不是“从 0 到 1”，而是继续把体验做稳、把 UI 做克制、把试戴质量和异步链路做实。
