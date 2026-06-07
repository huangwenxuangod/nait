# Nail-It Supabase 后端说明

这个目录存放 `指尖 SOP / Nail-It` 的 Supabase 后端能力，当前不是纯占位代码，而是一套已经参与主链路的 Edge Functions 实现。

## 目录目标

这一层负责承接 Android 客户端和模型服务之间的编排，包括：

1. 会话创建
2. 资源上传
3. 教程解析
4. AI 试戴任务发起与执行
5. BOM / SOP 生成
6. 实时链路 token 预留

## 当前函数列表

- `main`
- `create_session`
- `submit_source_link`
- `prepare_asset_upload`
- `confirm_asset_upload`
- `create_try_on`
- `render_try_on`
- `generate_execution_package`
- `create_qwen_temp_token`

## 函数职责

### `main`

统一路由入口，用于根据路径把请求分发到具体函数目录。

### `create_session`

创建一条新的 `session`，写入初始状态，作为整条流程的起点。

### `submit_source_link`

接收短视频链接或模板来源，推进 `source_parsing` 状态，并生成结构化的教程解析结果。

### `prepare_asset_upload`

为手图、教程图、模板图等资源生成上传目标路径。

### `confirm_asset_upload`

上传完成后的确认写回，更新 session / asset 状态。

### `create_try_on`

把会话推进到 `try_on_pending`，作为试戴任务入口。

### `render_try_on`

真正执行试戴渲染：

1. 拉取手图和模板图
2. 先做视觉分析
3. 生成结构化试戴 plan / prompt
4. 调用图片模型生成结果
5. 回写试戴图与 session 状态

### `generate_execution_package`

生成 BOM 和 SOP，供客户端进入“准备开始”和“带做”页面。

### `create_qwen_temp_token`

为后续实时语音/视频能力生成临时 token，目前属于后续实时链路的后端入口。

## 当前真实链路

现在的主链路不是“假接口”：

1. Android 创建 session
2. 提交来源链接或模板
3. 上传手图 / 模板图
4. 发起试戴
5. 生成试戴图
6. 生成执行包

但也要明确，当前依然处于 Demo 向产品化推进的阶段，主要问题在于：

- 试戴质量并不总是稳定
- 结构化输出有时依赖模型返回质量
- 某些链路仍需要更强的异步化和超时治理
- 实时功能入口已有，但最终体验还未彻底完成

## 共享模块

`_shared/` 下包含通用能力：

- `client.ts`：Supabase client
- `cors.ts`：CORS 处理
- `logger.ts`：统一日志
- `openai.ts`：OpenAI / 图片模型调用
- `qwen.ts`：Qwen 文本与视觉能力调用
- `types.ts`：公共请求/响应类型

## 模型分工

### Qwen

主要用于：

- 教程解析
- 多模态理解
- 结构化输出
- BOM / SOP 生成

常见变量：

- `QWEN_API_KEY`
- `QWEN_BASE_URL`
- `QWEN_TEXT_MODEL`
- `QWEN_VL_MODEL`

### OpenAI

主要用于：

- 试戴结果图生成

常见变量：

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_MODEL`
- `OPENAI_IMAGE_MODEL`

## 本地 / 服务器部署

如果你的 Supabase Edge Functions 是通过 Docker 挂载目录运行，推荐的更新方式：

```bash
cd ~/nait && git pull && rm -rf /root/supabase-docker/docker/volumes/functions/* && cp -a ~/nait/supabase/functions/. /root/supabase-docker/docker/volumes/functions/ && cd /root/supabase-docker/docker && docker compose up -d --force-recreate --no-deps functions && docker logs -f --tail 100 supabase-edge-functions
```

只重建函数容器则用：

```bash
cd /root/supabase-docker/docker && docker compose up -d --force-recreate --no-deps functions
```

## 推荐排查顺序

如果链路出问题，优先按这个顺序看：

1. `docker logs -f --tail 100 supabase-edge-functions`
2. 看 `main` 是否正确路由到对应函数
3. 看环境变量是否真正注入到容器
4. 看 `request_start / request_end` 日志
5. 看模型调用失败是 `401 / 403 / 502 / 超时` 哪一类
6. 看 session 状态是否写回成功

## 当前判断

这一层现在已经足够支撑 Demo 继续推进，不需要再把它当成“后面再搭的后端骨架”。更现实的工作重点是：

1. 提高试戴效果质量
2. 提高结构化输出稳定性
3. 把长耗时任务做得更异步
4. 继续增强日志、回退和失败提示
