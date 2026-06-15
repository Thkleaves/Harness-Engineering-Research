# Harness Engineering 调研实践

> **状态**: 完成 | **日期**: 2026-06-09 ~ 2026-06-15 | **阶段**: 数据汇总 + 团队分享

---

## 📋 项目概述

本项目对 **Harness Engineering（工程化框架）** 领域进行系统性实操调研，通过 12 个 Spring Boot 任务的量化评测 + URL 短链接服务的过程评测，评估 5 种工作模式（Baseline / Superpowers / Gstack / OpenSpec / wow-harness）的实际效果。

Harness Engineering 是指围绕 AI 编码代理（Coding Agent）构建的**工程化基础设施**——包括上下文管理、工具治理、安全护栏、可观测性、记忆系统、编排调度等。

### 核心发现

> **Gstack 以 1090 分排名第一，wow-harness 以 1085 分紧随其后（仅差 5 分）。**  
> Superpowers 未能击败裸 Agent（1045 = 1045）——**流程步骤多 ≠ 质量好**，Agent 平均只调用了 3-4/14 技能。  
> 自动评测中禁止用户交互，系统性低估了 Harness 的价值——过程评测四轮对比证实了这一点。

---

## 📁 目录结构

```
Harness/
├── README.md                          ← 本文件
├── docs/                              ← 全部文档
│   ├── plans/                         ← 调研文档 + 4 天冲刺计划
│   │   ├── 01-概念与背景.md
│   │   ├── 02-工具调研与对比.md
│   │   ├── 03-工具实操指南/
│   │   ├── 04-实践对比分析.md
│   │   ├── 05-团队使用建议.md
│   │   ├── 06-评测方案设计.md
│   │   └── PLAN.md
│   ├── records/                       ← Day 1 实操记录
│   │   ├── 任务③-Superpowers全流程总结.md
│   │   ├── 任务④-裸Agent对比记录.md
│   │   ├── 任务⑤-kit-10维度评审.md
│   │   ├── 任务⑥-HarnessForge试用.md
│   │   ├── 任务⑦-Superpowers-L2分页排序.md
│   │   └── 任务⑧-踩坑清单.md
│   └── share/                         ← 团队分享文档
│       ├── Part1-问题.md
│       ├── Part2-方法.md
│       ├── Part3-数据.md
│       └── Part4-总结.md
├── eval/                              ← 评测框架
│   ├── scripts/                       ← 自动化脚本
│   │   ├── run-eval.sh                ← 主评测 runner（支持 l1/l2a/l2b/l3/l4 模式）
│   │   ├── verify.sh                  ← 评分脚本（编译 gate + 测试 + 任务断言）
│   │   ├── rebuild-csv.sh             ← 批量重验 + CSV 重建
│   │   ├── run-provider.sh            ← 单 Provider 执行器
│   │   └── score.js                   ← 评分逻辑（JS）
│   ├── providers/                     ← Provider 配置文件（5 个）
│   │   ├── baseline.md / superpowers.md / gstack.md / openspec.md / wow-harness.md
│   ├── tasks/                         ← 12 个任务 spec（YAML）
│   ├── workspaces/                    ← 12 个定量评测工作区
│   │   ├── 01-validation/ ~ 12-flaky-test/
│   ├── results/                       ← 评测产出
│   │   ├── eval-results.csv           ← 60 条完整数据（12 任务 × 5 Provider）
│   │   ├── README.md                  ← 评测总结（得分表 + 6 大核心发现）
│   │   ├── task-01-validation.md ~ task-12-flaky-test.md  ← 12 篇逐任务分析
│   │   ├── process-analysis-url-shortener.md               ← 过程评测四轮对比
│   │   ├── generate_charts.py         ← Python 图表生成脚本
│   │   └── charts/                    ← 4 张可视化图表（PNG）
│   └── promptfooconfig.yaml           ← Promptfoo 框架配置
└── trials/                            ← Day 1 试用 + 过程评测工作区
    ├── 01-book-api/                   ← Superpowers 全流程（L1 校验 + L2 分页排序）
    ├── 02-book-api-bare/              ← 裸 Agent 对照
    ├── 03-book-api-kit-review/        ← agent-harness-kit 评审
    ├── 04-book-api-forge/             ← HarnessForge 试用
    ├── url-shortener-bare/            ← 过程评测 A：裸 Agent（13 测试）
    ├── url-shortener-harness/         ← 过程评测 B/C：Harness 自动+交互
    └── url-shortener-wow/             ← 过程评测 D：wow-harness（17 测试，含 spec）
```

---

## 🎯 调研范围

| 维度 | 覆盖 |
|------|------|
| **量化评测** | 12 个 Spring Boot 任务 × 5 组 Provider = 60 条记录 |
| **过程评测** | URL 短链接服务四轮对比（裸Agent / Harness自动 / Harness交互 / wow-harness） |
| **Provider** | Baseline(裸Agent)、Superpowers(v5.1.0)、Gstack、OpenSpec、wow-harness |
| **模型** | DeepSeek-V4-pro |
| **实验项目** | Spring Boot 3.4 + Java 21 + Maven |
| **评估维度** | 编译通过率、测试覆盖、任务断言完成度、代码质量、Token 效率、架构决策、可追溯性 |

---

## 🔑 核心结论

### 1. 量化评测：Gstack 最优，wow-harness 紧随

| Provider | 总分 | 平均分 | 满分次数 |
|----------|:---:|:---:|:---:|
| **Gstack** | **1090** | **90.8** | 2 |
| **wow-harness** | **1085** | **90.4** | 2 |
| OpenSpec | 1065 | 88.8 | 2 |
| Baseline（裸Agent） | 1045 | 87.1 | 2 |
| Superpowers | 1045 | 87.1 | 2 |

- Gstack 的角色分工（CEO → Engineer → QA → DevOps）比严格流程更有效；wow-harness 硬约束 + 治理框架仅差 5 分
- Superpowers 流程最繁重但得分与裸 Agent 持平——**流程步骤多 ≠ 质量好**
- 任务难度越大，Harness 价值越高（L4 中 Gstack 在 N+1 修复上领先 Baseline 25 分）

### 2. 过程评测：四轮对比揭示 Harness 的本质差异

URL 短链接服务四轮对比（裸Agent / Harness自动 / Harness交互 / wow-harness）：

- **自动 Harness = 裸 Agent**：Base62 自增、不去重——自问自答没有改变核心决策
- **交互 Harness ≠ 裸 Agent**：用户 3 轮决策 → 随机 7 位、24h 去重、完整日志、Clock 注入
- **wow-harness 的独特定位**：先诊断现有代码 → 写 spec → 用户确认 → 只修 3 个缺陷。四轮中唯一产出设计文档、改动范围最小（3 文件 vs 10-16 文件）

### 3. Token 效率差异巨大

Superpowers 的总 token 消耗是 Gstack 的 1.5-2 倍，但得分不增。流程繁重 = 成本增加 ≠ 质量提升。

### 4. 软约束 vs 硬约束：遵从率的鸿沟

- CLAUDE.md 文本指令的 Agent 遵从率 ~20%：Superpowers 14 技能中 Agent 平均只调用了 3-4 个，关键技能（TDD、review）在部分任务上被跳过
- wow-harness hooks 机械执行率 100%：SessionStart/End hooks 正常运行，但 PreToolUse/PostToolUse 在 Windows 批量模式下兼容性受限

### 5. Agent 共性问题

- 倾向"加新代码"而非"改旧代码"（Flaky 测试五组全部失败于未移除 LocalDate.now()）
- 自动场景下架构决策趋同（缺乏外部信息输入时只能选"合理默认值"）

---

## 🚀 进度回顾

```
Day 1 (06/09) ✅ 工具深潜 + 实操跑通——8/8 任务完成，3 个工具试用，11 条踩坑清单
Day 2 (06/10) ✅ 评测基础设施——CRLF 修复、verify.sh 加固、L1+L2 全量 26 条数据
Day 3 (06/11) ✅ L3+L4 全量 + 过程评测 + 分享文档——48 条完整数据、12 篇任务分析、
                     URL 短链接三轮对比、Part1-4 分享文档
Day 4 (06/12) ✅ 数据可视化——Python matplotlib 出 4 张图、嵌入分享文档、目录整理
Day 5 (06/15) ✅ wow-harness 评测——量化 12 任务 1085 分(排名第二)、过程评测第四轮、
                     Superpowers 技能调用率分析、全部文档更新

Day 6 (06/15) ✅ Reasonix 沙箱验证 + 评分框架修正——3/3 任务沙箱重跑与历史 wow-harness
                     一致（01=95/02=90/12=70）、verify.sh 逐任务质量适配、
                     任务⑫ LocalDate.now 误判修复、全部可达 100 分
```

---

## 📚 参考资源

- [Awesome Harness Engineering (walkinglabs)](https://github.com/walkinglabs/awesome-harness-engineering)
- [Awesome Harness Engineering (ai-boost)](https://github.com/ai-boost/awesome-harness-engineering)
- [wow-harness](https://github.com/NatureBlueee/wow-harness) — Claude Code 治理层框架（16 hooks + 8 关状态机）
- [OpenAI: Harness Engineering Paper (Feb 2026)](https://cdn.openai.com/agent-harness-engineering.pdf)
- [LangChain: Deep Agents on Terminal-Bench](https://blog.langchain.dev/deep-agents-terminal-bench/)
- [Simon Willison: Superpowers Review](https://simonwillison.net/2025/Oct/10/superpowers/)
