import type { TutorialData } from "@/lib/types";

/**
 * 视频录制与剪辑模块 (Remotion 版)
 *
 * 预览：使用 @remotion/player 在浏览器内实时预览
 * 渲染：运行 npx remotion render 命令导出 mp4
 */

export interface VideoOptions {
  data: TutorialData;
  stepImages: string[];
  bgmUrl?: string;
}

export interface VideoResult {
  compositionId: string;
  inputProps: Record<string, unknown>;
  fps: number;
  durationInFrames: number;
  width: number;
  height: number;
}

const FPS = 30;
const INTRO_SEC = 2;
const STEP_SEC = 5;
const OUTRO_SEC = 3;

/**
 * 计算视频总时长和 Remotion 渲染参数
 * 用于 @remotion/player 的 <Player> 组件
 */
export function getVideoConfig(opts: VideoOptions): VideoResult {
  const totalSteps = opts.data.steps.length;
  const totalSec = INTRO_SEC + totalSteps * STEP_SEC + OUTRO_SEC;
  const durationInFrames = totalSec * FPS;

  return {
    compositionId: "nail-tutorial",
    inputProps: {
      data: opts.data,
      stepImages: opts.stepImages,
      bgmUrl: opts.bgmUrl ?? "",
    },
    fps: FPS,
    durationInFrames,
    width: 1080,
    height: 1920,
  };
}

/**
 * Mock 版本：预览用 getVideoConfig，真正渲染用 CLI
 *
 * 命令行渲染：
 *   npx remotion render nail-tutorial \
 *     --props='{"data":...,"stepImages":[...]}' \
 *     output.mp4
 *
 * 或在 package.json 加脚本：
 *   "render": "remotion render src/remotion/index.ts nail-tutorial output.mp4"
 */

/**
 * 占位：生成视频（浏览器预览用 Player 替代，真正渲染用 CLI）
 * 保留此函数用于 FocusScreen 流程兼容
 */
export async function generateVideo(opts: VideoOptions): Promise<VideoResult> {
  // 模拟 AI 剪辑延迟
  await new Promise((r) => setTimeout(r, 2000));
  return getVideoConfig(opts);
}

/* ── 分享接口 ── */

/**
 * 跳转抖音
 * 优先唤起 App（snssdk1128://），未安装则打开网页版
 */
export function shareToDouyin(): void {
  const webUrl = "https://www.douyin.com";
  const appScheme = "snssdk1128://";

  // 先尝试跳转 App
  const iframe = document.createElement("iframe");
  iframe.style.display = "none";
  iframe.src = appScheme;
  document.body.appendChild(iframe);

  // 2 秒后如果 App 没唤起，跳转网页
  setTimeout(() => {
    document.body.removeChild(iframe);
    window.open(webUrl, "_blank");
  }, 2000);
}

export function shareToXiaohongshu(): void {
  window.open("https://www.xiaohongshu.com", "_blank");
}

export function shareToWechat(): void {
  // 微信分享需要 JSSDK，Demo 用系统分享 API
  if (navigator.share) {
    navigator.share({
      title: "我的美甲 DIY 视频",
      text: "用指尖 SOP 完成了第一款 AI 辅助美甲！",
    }).catch(() => {});
  } else {
    window.open("https://web.wechat.com", "_blank");
  }
}

/* ── 录制 & 截图 ── */

export function startRecording(): Promise<MediaRecorder | null> {
  console.log("[录制] 开始录制");
  return Promise.resolve(null);
}

export function stopRecording(recorder: MediaRecorder | null): Promise<Blob | null> {
  console.log("[录制] 停止录制");
  return Promise.resolve(null);
}

export function captureStepFrame(videoEl: HTMLVideoElement): string | null {
  if (!videoEl) return null;
  try {
    const canvas = document.createElement("canvas");
    canvas.width = videoEl.videoWidth;
    canvas.height = videoEl.videoHeight;
    const ctx = canvas.getContext("2d");
    if (!ctx) return null;
    ctx.drawImage(videoEl, 0, 0);
    return canvas.toDataURL("image/jpeg", 0.8);
  } catch {
    return null;
  }
}
