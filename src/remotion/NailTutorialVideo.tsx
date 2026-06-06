import { AbsoluteFill, Sequence, useCurrentFrame, useVideoConfig, interpolate, spring, Img, Audio } from "remotion";
import type { TutorialData } from "@/lib/types";

interface Props {
  data: TutorialData;
  stepImages: string[]; // base64 or URL for each step
  bgmUrl?: string;
}

const BRAND = "#D4A3A3";
const ACCENT = "#A8D5BA";
const DARK = "#1A1A1A";

export const NailTutorialVideo: React.FC<Props> = ({ data, stepImages, bgmUrl }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const introDuration = 2 * fps;   // 2s intro
  const stepDuration = 5 * fps;    // 5s per step
  const outroDuration = 3 * fps;   // 3s outro

  const totalSteps = data.steps.length;
  const introEnd = introDuration;
  const outroStart = introDuration + totalSteps * stepDuration;

  // Intro: cover slide
  const introFade = interpolate(frame, [introEnd - 30, introEnd], [1, 0], { extrapolateRight: "clamp" });

  return (
    <AbsoluteFill style={{ backgroundColor: DARK }}>
      {bgmUrl && <Audio src={bgmUrl} />}

      {/* Intro */}
      <Sequence from={0} durationInFrames={introDuration}>
        <IntroSlide data={data} opacity={introFade} />
      </Sequence>

      {/* Steps */}
      {data.steps.map((step, i) => {
        const stepStart = introEnd + i * stepDuration;
        const stepEnd = stepStart + stepDuration;
        return (
          <Sequence key={i} from={stepStart} durationInFrames={stepDuration}>
            <StepSlide
              step={step}
              image={stepImages[i] ?? stepImages[0]}
              stepIndex={i}
              fps={fps}
              stepDuration={stepDuration}
            />
          </Sequence>
        );
      })}

      {/* Outro */}
      <Sequence from={outroStart} durationInFrames={outroDuration}>
        <OutroSlide data={data} fps={fps} duration={outroDuration} />
      </Sequence>
    </AbsoluteFill>
  );
};

/* ── Intro ── */
function IntroSlide({ data, opacity }: { data: TutorialData; opacity: number }) {
  const frame = useCurrentFrame();
  const titleSpring = spring({ frame, fps: 30, config: { damping: 12 } });

  return (
    <AbsoluteFill
      style={{
        opacity,
        justifyContent: "center",
        alignItems: "center",
        padding: 80,
        background: `linear-gradient(135deg, ${DARK} 0%, #2A1A1A 100%)`,
      }}
    >
      <div
        style={{
          fontSize: 36,
          fontWeight: 300,
          color: BRAND,
          letterSpacing: "0.15em",
          transform: `scale(${titleSpring})`,
          textAlign: "center",
          marginBottom: 30,
        }}
      >
        指尖 SOP
      </div>
      <div
        style={{
          fontSize: 52,
          fontWeight: 200,
          color: "#FFF",
          letterSpacing: "0.08em",
          textAlign: "center",
          lineHeight: 1.3,
          marginBottom: 20,
        }}
      >
        {data.videoTitle}
      </div>
      <div
        style={{
          fontSize: 22,
          color: "rgba(255,255,255,0.5)",
          fontWeight: 300,
          marginTop: 20,
        }}
      >
        {data.style}
      </div>
      <div
        style={{
          position: "absolute",
          bottom: 60,
          fontSize: 18,
          color: "rgba(255,255,255,0.3)",
          letterSpacing: "0.2em",
        }}
      >
        AI 辅助美甲 · 沉浸式 DIY
      </div>
    </AbsoluteFill>
  );
}

/* ── Step slide ── */
function StepSlide({
  step,
  image,
  stepIndex,
  fps,
  stepDuration,
}: {
  step: TutorialData["steps"][0];
  image: string;
  stepIndex: number;
  fps: number;
  stepDuration: number;
}) {
  const frame = useCurrentFrame();

  const slideIn = spring({ frame, fps, config: { damping: 14 }, delay: 4 });
  const fadeIn = interpolate(frame, [0, 15], [0, 1], { extrapolateRight: "clamp" });
  const progressBar = Math.min(frame / stepDuration, 1);

  return (
    <AbsoluteFill>
      {/* Background image */}
      {image && (
        <Img
          src={image}
          style={{
            position: "absolute",
            inset: 0,
            width: "100%",
            height: "100%",
            objectFit: "cover",
            opacity: 0.35,
            filter: "blur(8px)",
          }}
        />
      )}

      {/* Content */}
      <AbsoluteFill style={{ padding: "60px 50px", justifyContent: "flex-end" }}>
        {/* Step number */}
        <div
          style={{
            fontSize: 14,
            letterSpacing: "0.3em",
            color: BRAND,
            marginBottom: 12,
            opacity: fadeIn,
          }}
        >
          STEP {String(step.step).padStart(2, "0")} / {String(step.total).padStart(2, "0")}
        </div>

        {/* Title */}
        <div
          style={{
            fontSize: 18,
            fontWeight: 400,
            color: "rgba(255,255,255,0.5)",
            letterSpacing: "0.15em",
            marginBottom: 8,
            opacity: fadeIn,
          }}
        >
          {step.title}
        </div>

        {/* Instruction */}
        <div
          style={{
            fontSize: 42,
            fontWeight: 200,
            color: "#FFF",
            lineHeight: 1.3,
            transform: `translateY(${(1 - slideIn) * 40}px)`,
            opacity: slideIn,
            marginBottom: 40,
          }}
        >
          {step.instruction}
        </div>

        {/* AI translation */}
        <div
          style={{
            backgroundColor: "rgba(168,213,186,0.1)",
            border: "1px solid rgba(168,213,186,0.2)",
            borderRadius: 20,
            padding: "20px 24px",
            opacity: fadeIn,
          }}
        >
          <div style={{ fontSize: 12, letterSpacing: "0.2em", color: ACCENT, marginBottom: 8 }}>
            ✨ AI 解读
          </div>
          <div style={{ fontSize: 20, color: "rgba(255,255,255,0.8)", lineHeight: 1.6 }}>
            {step.aiTranslation}
          </div>
        </div>

        {/* Progress bar */}
        <div
          style={{
            marginTop: 30,
            height: 2,
            backgroundColor: "rgba(255,255,255,0.1)",
            borderRadius: 1,
            overflow: "hidden",
          }}
        >
          <div
            style={{
              height: "100%",
              width: `${progressBar * 100}%`,
              backgroundColor: BRAND,
              borderRadius: 1,
            }}
          />
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
}

/* ── Outro ── */
function OutroSlide({ data, fps, duration }: { data: TutorialData; fps: number; duration: number }) {
  const frame = useCurrentFrame();
  const fadeIn = interpolate(frame, [0, 20], [0, 1], { extrapolateRight: "clamp" });

  return (
    <AbsoluteFill
      style={{
        justifyContent: "center",
        alignItems: "center",
        padding: 80,
        background: `linear-gradient(135deg, ${BRAND} 0%, #EBD8B8 100%)`,
      }}
    >
      <div style={{ fontSize: 48, fontWeight: 300, color: "#FFF", textAlign: "center", opacity: fadeIn, marginBottom: 16 }}>
        ✨
      </div>
      <div style={{ fontSize: 28, fontWeight: 200, color: "#FFF", textAlign: "center", marginBottom: 40, opacity: fadeIn, letterSpacing: "0.1em" }}>
        你也来试试吧
      </div>
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          justifyContent: "center",
          gap: 12,
          opacity: fadeIn,
        }}
      >
        {["美甲DIY", "指尖SOP", "沉浸式美甲", "自学美甲"].map((tag) => (
          <div
            key={tag}
            style={{
              padding: "8px 20px",
              borderRadius: 20,
              backgroundColor: "rgba(255,255,255,0.2)",
              color: "#FFF",
              fontSize: 16,
            }}
          >
            #{tag}
          </div>
        ))}
      </div>
      <div
        style={{
          position: "absolute",
          bottom: 50,
          fontSize: 16,
          color: "rgba(255,255,255,0.5)",
          letterSpacing: "0.15em",
          opacity: fadeIn,
        }}
      >
        打开指尖 SOP · 粘贴教程链接开始
      </div>
    </AbsoluteFill>
  );
}
