import type { ReactNode } from "react";

export function PhoneFrame({ children, dark = false }: { children: ReactNode; dark?: boolean }) {
  return (
    <div className="min-h-screen w-full flex justify-center" style={{ backgroundColor: "#E5E5E5" }}>
      <div
        className="w-full max-w-[430px] min-h-screen relative overflow-hidden shadow-[0_8px_40px_-12px_rgba(0,0,0,0.15)]"
        style={{ backgroundColor: dark ? "#1A1A1A" : "#FAFAFA" }}
      >
        {children}
      </div>
    </div>
  );
}
