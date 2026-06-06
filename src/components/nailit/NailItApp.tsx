import { useEffect, useRef, useState } from "react";
import type { TutorialData } from "@/lib/types";
import { HomeScreen } from "./HomeScreen";
import { TryOnScreen } from "./TryOnScreen";
import { PrepScreen } from "./PrepScreen";
import { FocusScreen } from "./FocusScreen";

type Screen = "home" | "tryon" | "prep" | "focus";

export function NailItApp() {
  const [screen, setScreen] = useState<Screen>("home");
  const [handImage, setHandImage] = useState<string | null>(null);
  const [missingItems, setMissingItems] = useState<string[]>([]);
  const [tutorialData, setTutorialData] = useState<TutorialData | null>(null);

  const screenKey = useRef(0);

  const navigate = (next: Screen) => {
    screenKey.current += 1;
    setScreen(next);
  };

  const render = (children: React.ReactNode) => (
    <div
      key={screenKey.current}
      style={{
        animation: "screen-fade-in 0.25s ease-out",
      }}
    >
      {children}
    </div>
  );

  switch (screen) {
    case "home":
      return render(
        <HomeScreen
          handImage={handImage}
          onHandChange={setHandImage}
          onParseComplete={(data) => {
            setTutorialData(data);
            navigate("tryon");
          }}
          onQuickStart={() => navigate("tryon")}
        />,
      );
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
          onExit={() => navigate("home")}
          missingItems={missingItems}
          tutorialData={tutorialData}
        />,
      );
  }
}
