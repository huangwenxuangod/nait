import { useEffect, useRef, useState } from "react";
import type { TutorialData } from "@/lib/types";
import { HomeScreen } from "./HomeScreen";
import { TryOnScreen } from "./TryOnScreen";
import { PrepScreen } from "./PrepScreen";
import { FocusScreen } from "./FocusScreen";
import { InspirationScreen } from "./InspirationScreen";
import { ProfileScreen } from "./ProfileScreen";
import { Sparkles } from "lucide-react";

type Screen = "home" | "inspiration" | "profile" | "tryon" | "prep" | "focus";

function TabBar({ current, onChange }: { current: Screen; onChange: (s: Screen) => void }) {
  const tabs = [
    { id: "home" as Screen, icon: <HomeIcon />, label: "首页" },
    { id: "inspiration" as Screen, icon: <Sparkles className="w-5 h-5" strokeWidth={1.6} />, label: "灵感" },
    { id: "profile" as Screen, icon: <UserIcon />, label: "我的" },
  ];
  return (
    <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[430px] bg-white/95 backdrop-blur-md border-t border-border/60 pt-2 pb-6 px-8 flex items-center justify-between z-40">
      {tabs.map((t) => (
        <button
          key={t.id}
          onClick={() => onChange(t.id)}
          className={`flex flex-col items-center gap-0.5 ${current === t.id ? "text-brand" : "text-foreground/40"}`}
        >
          {t.icon}
          <span className="text-[10px] tracking-wider">{t.label}</span>
        </button>
      ))}
    </div>
  );
}

function HomeIcon() {
  return (
    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 11l9-8 9 8" /><path d="M5 10v10h14V10" />
    </svg>
  );
}
function UserIcon() {
  return (
    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="8" r="4" /><path d="M4 21c1.5-4 5-6 8-6s6.5 2 8 6" />
    </svg>
  );
}

export function NailItApp() {
  const [screen, setScreen] = useState<Screen>("home");
  const [handImage, setHandImage] = useState<string | null>(null);
  const [missingItems, setMissingItems] = useState<string[]>([]);
  const [tutorialData, setTutorialData] = useState<TutorialData | null>(null);
  const [wishlist, setWishlist] = useState<string[]>([]);
  const [completed, setCompleted] = useState<{ title: string; date: string; steps: number }[]>([]);
  const screenKey = useRef(0);

  const navigate = (next: Screen) => {
    screenKey.current += 1;
    setScreen(next);
  };

  const render = (children: React.ReactNode) => (
    <div
      key={screenKey.current}
      style={{ animation: "screen-fade-in 0.25s ease-out" }}
    >
      {children}
    </div>
  );

  const showTab = screen === "home" || screen === "inspiration" || screen === "profile";

  return (
    <div style={{ position: "relative" }}>
      {(() => {
        switch (screen) {
          case "home":
            return render(
              <HomeScreen
                handImage={handImage}
                onHandChange={setHandImage}
                onParseComplete={(data) => {
                  setTutorialData(data);
                  setWishlist((w) => (w.includes(data.videoTitle) ? w : [data.videoTitle, ...w]));
                  navigate("tryon");
                }}
                onQuickStart={() => navigate("tryon")}
              />,
            );
          case "inspiration":
            return render(
              <InspirationScreen
                onSelectVideo={(title) => {
                  setWishlist((w) => (w.includes(title) ? w : [title, ...w]));
                }}
              />,
            );
          case "profile":
            return render(<ProfileScreen wishlist={wishlist} completed={completed} />);
          case "tryon":
            return render(
              <TryOnScreen
                handImage={handImage}
                tutorialData={tutorialData}
                onBack={() => navigate("home")}
                onConfirm={() => {
                  setMissingItems([]);
                  navigate("prep");
                }}
              />,
            );
          case "prep":
            return render(
              <PrepScreen
                tutorialData={tutorialData}
                onBack={() => navigate("tryon")}
                onStart={(missing) => {
                  setMissingItems(missing);
                  navigate("focus");
                }}
              />,
            );
          case "focus":
            return render(
              <FocusScreen
                onExit={() => {
                  if (tutorialData) {
                    const today = new Date().toISOString().slice(0, 10);
                    setCompleted((c) => [{ title: tutorialData.videoTitle, date: today, steps: tutorialData.steps.length }, ...c]);
                  }
                  navigate("home");
                }}
                missingItems={missingItems}
                tutorialData={tutorialData}
              />,
            );
        }
      })()}

      {showTab && <TabBar current={screen} onChange={navigate} />}
    </div>
  );
}
