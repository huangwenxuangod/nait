import { createFileRoute } from "@tanstack/react-router";
import { NailItApp } from "@/components/nailit/NailItApp";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "指尖 SOP · Nail-It — AI 美甲 DIY 助手" },
      { name: "description", content: "粘贴教程链接，AI 提取款式与步骤，虚拟试戴，沉浸式 SOP 引导你完成专属美甲。" },
      { property: "og:title", content: "指尖 SOP · Nail-It" },
      { property: "og:description", content: "AI-powered nail art DIY assistant — extract, try on, and follow hands-free." },
    ],
  }),
  component: NailItApp,
});
