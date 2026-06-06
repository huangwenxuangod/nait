import { useState } from "react";
import { HomeScreen } from "./HomeScreen";
import { TryOnScreen } from "./TryOnScreen";
import { PrepScreen } from "./PrepScreen";
import { FocusScreen } from "./FocusScreen";

type Screen = "home" | "tryon" | "prep" | "focus";

export function NailItApp() {
  const [screen, setScreen] = useState<Screen>("home");
  const [handImage, setHandImage] = useState<string | null>(null);

  switch (screen) {
    case "home":
      return (
        <HomeScreen
          handImage={handImage}
          onHandChange={setHandImage}
          onNext={() => setScreen("tryon")}
        />
      );
    case "tryon":
      return (
        <TryOnScreen
          handImage={handImage}
          onBack={() => setScreen("home")}
          onConfirm={() => setScreen("prep")}
        />
      );
    case "prep":
      return <PrepScreen onBack={() => setScreen("tryon")} onStart={() => setScreen("focus")} />;
    case "focus":
      return <FocusScreen onExit={() => setScreen("home")} />;
  }
}
