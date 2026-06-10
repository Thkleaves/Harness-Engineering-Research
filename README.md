# Harness Engineering 调研实践

> **状态**: Day 1 完成 | **日期**: 2026-06-09 | **当前阶段**: 工具深潜 + 实操数据采集

---

## 📋 项目概述

本项目对 **Harness Engineering（工程化框架）** 领域进行系统性实操调研，通过对照实验评估 Superpowers、agent-harness-kit、HarnessForge 三个核心工具的实际效果，最终产出有数据支撑的团队技术分享。

Harness Engineering 是指围绕 AI 编码代理（Coding Agent）构建的**工程化基础设施**——包括上下文管理、工具治理、安全护栏、可观测性、记忆系统、编排调度等。

### 核心论点

> **AI 编码能力的瓶颈已从模型能力转向工程化框架质量。**  
> 同一模型在不同 Harness 下表现差异显著。LangChain 在 Terminal-Bench 2.0 上证实 Harness 优化（+13.7%）ROI 高于模型升级（+5.2%）。  
> *"Agent = Model + Harness"* — 模型是引擎，Harness 是让它可控行驶的底盘。

---

## 📁 目录结构

```
Harness/
├── README.md                          ← 本文件
├── plans/                             ← 调研文档 + 4 天冲刺计划
│   ├── 01-概念与背景.md                ← 什么是 Harness Engineering
│   ├── 02-工具调研与对比.md             ← 工具全景调研与横向对比
│   ├── 03-工具实操指南/                ← 各工具实操指南
│   │   ├── 03-01-Superpowers.md
│   │   ├── 03-02-Agent-Harness-Kit.md
│   │   └── 03-03-HarnessForge.md
│   ├── 04-实践对比分析.md               ← 实操对比分析
│   ├── 05-团队使用建议.md               ← 团队落地建议与路线图
│   ├── 06-评测方案设计.md               ← 评测框架设计
│   └── PLAN.md                        ← 4 天冲刺详细任务表
├── records/                           ← Day 1 实操记录
│   ├── 任务③-Superpowers全流程总结.md    ← Superpowers 全流程跑通记录
│   ├── 任务④-裸Agent对比记录.md          ← 裸 Agent vs Superpowers 对照实验
│   ├── 任务⑤-kit-10维度评审.md           ← agent-harness-kit 10 维度评审
│   ├── 任务⑥-HarnessForge试用.md          ← HarnessForge init 试用
│   ├── 任务⑦-Superpowers-L2分页排序.md     ← Superpowers L2 功能
│   └── 任务⑧-踩坑清单.md                 ← 11 条踩坑经验
└── workspaces/                        ← 评测用项目副本
    ├── 01-book-api/                   ← Superpowers 全流程（L1 校验 + L2 分页排序）
    ├── 02-book-api-bare/              ← 裸 Agent 对照（仅基础校验）
    ├── 03-book-api-kit-review/        ← agent-harness-kit 评审工作区
    └── 04-book-api-forge/             ← HarnessForge 试用工作区
```

---

## 🎯 调研范围

| 维度 | 覆盖 |
|------|------|
| **核心工具** | Superpowers (v5.1.0)、agent-harness-kit (v0.22.3)、HarnessForge (v0.2.2) |
| **参考工具** | Agent Harness (Go)、HarnessFlow、Harness Skills、OpenHarness |
| **调研方式** | 官方文档研读 + 实操 A/B 对照实验 + 多维评审 + 踩坑记录 |
| **实验项目** | Spring Boot 3.4 + Java 21 Book CRUD API（内存存储） |
| **评估维度** | 功能完整度、上手难度、代码质量产出、测试覆盖、文档产出、Token 效率 |

---

## 🔑 Day 1 核心发现

### 1. brainstorming 是 Superpowers 最被低估的价值点

两次实验（L1 校验 + L2 分页排序），Superpowers 通过 5-6 轮苏格拉底式追问把一句模糊需求变成了可执行的 spec。裸 Agent 对照实验验证了这一点——没有 brainstorming 时只做表面校验（`@NotBlank`），遗漏了 ISBN 格式、价格小数位、PUT 部分更新策略。

### 2. 三个工具定位互补，不是竞品

```
HarnessForge → "3 秒让项目 Agent Ready"（配置骨架，~20 文件）
Superpowers → "完整开发流程"（brainstorm → spec → plan → TDD → review）
agent-harness-kit → "生产级安全护栏"（10 维度评审 + Failures→Rules + Hook 系统）
```

推荐叠加路径：Forge init → Superpowers 插件 → kit 在关键 PR 做深度评审。

### 3. 架构决策是模型做的，Harness 影响的是深度

Superpowers 和裸 Agent 在同一个需求上选择了完全相同的架构方案（Jakarta Bean Validation 直接注解实体），说明这个层级的判断力来自模型本身。差异在：Superpowers 追问了 6 个具体选择，做出了 ISBN 格式校验和价格小数位校验；裸 Agent 停在 `@NotBlank` + `@NotNull`。

### 4. 子代理在非 Claude 模型下不稳定

Subagent-Driven Development 在 DeepSeek 模型下三个子代理全部失败（API 参数不兼容），需 Inline Execution 作为回退。

---

## 🚀 4 天冲刺进度

```
Day 1 ✅ 工具深潜 + 亲手跑通（8/8 任务完成）
Day 2 ⬜ 评测基础设施 + L1/L2 全量跑（目标 52 条数据）
Day 3 ⬜ L3/L4 全量跑 + 数据汇总（目标 120+ 条数据）
Day 4 ⬜ 数据可视化 + Slide 制作 + 团队分享
```

详细任务见 [plans/PLAN.md](plans/PLAN.md)。

## 📊 Day 1 产出统计

| 指标 | 数值 |
|------|:----:|
| 实操工具 | 3 个 |
| 功能跑通 | 2 个（L1 校验 + L2 分页排序） |
| 工作区 | 4 个 |
| 记录文档 | 8 篇 |
| 踩坑清单 | 11 条（阻塞 2 / 阻碍 5 / 轻度 4） |
| 单元测试 | 23 个（Superpowers L2） |
| Git commits | 7 个（Superpowers L2） |

## 📚 参考资源

- [Awesome Harness Engineering (walkinglabs)](https://github.com/walkinglabs/awesome-harness-engineering)
- [Awesome Harness Engineering (ai-boost)](https://github.com/ai-boost/awesome-harness-engineering)
- [OpenAI: Harness Engineering Paper (Feb 2026)](https://cdn.openai.com/agent-harness-engineering.pdf)
- [LangChain: Deep Agents on Terminal-Bench](https://blog.langchain.dev/deep-agents-terminal-bench/)
- [Simon Willison: Superpowers Review](https://simonwillison.net/2025/Oct/10/superpowers/)
