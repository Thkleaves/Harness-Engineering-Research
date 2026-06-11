# Harness Engineering 调研实践

> **状态**: 完成 | **日期**: 2026-06-09 ~ 2026-06-11 | **阶段**: 数据汇总 + 团队分享

---

## 📋 项目概述

本项目对 **Harness Engineering（工程化框架）** 领域进行系统性实操调研，通过 12 个 Spring Boot 任务的量化评测 + URL 短链接服务的过程评测，评估 Bare Agent vs Superpowers vs Gstack vs OpenSpec 四种工作模式的实际效果。

Harness Engineering 是指围绕 AI 编码代理（Coding Agent）构建的**工程化基础设施**——包括上下文管理、工具治理、安全护栏、可观测性、记忆系统、编排调度等。

### 核心发现

> **Superpowers 并未击败裸 Agent（总分 1045 = 1045）。**  
> Gstack 以 1090 分排名第一，证明**角色分工 > 流程约束**。  
> 自动评测中禁止用户交互，系统性低估了 Harness 在**需求澄清阶段**的核心价值——用户的 3 轮决策可以彻底改变架构选型（过程评测证实）。

---

## 📁 目录结构

```
Harness/
├── README.md                          ← 本文件
├── plans/                             ← 调研文档 + 4 天冲刺计划
│   ├── 01-概念与背景.md                ← 什么是 Harness Engineering
│   ├── 02-工具调研与对比.md             ← 工具全景调研与横向对比
│   ├── 04-实践对比分析.md               ← 实操对比分析
│   ├── 05-团队使用建议.md               ← 团队落地建议与路线图
│   ├── 06-评测方案设计.md               ← 评测框架设计
│   └── PLAN.md                        ← 4 天冲刺详细任务表
├── records/                           ← Day 1 实操记录
│   ├── 任务③-Superpowers全流程总结.md
│   ├── 任务④-裸Agent对比记录.md
│   ├── 任务⑤-kit-10维度评审.md
│   ├── 任务⑥-HarnessForge试用.md
│   ├── 任务⑦-Superpowers-L2分页排序.md
│   └── 任务⑧-踩坑清单.md
├── eval/                              ← 评测框架
│   ├── scripts/                       ← 自动化脚本
│   │   ├── run-eval.sh                ← 主评测 runner（支持 l1/l2a/l2b/l3a/l3b/l4 模式）
│   │   ├── verify.sh                  ← 评分脚本（编译 gate + 测试 + 任务断言）
│   │   ├── rebuild-csv.sh             ← 批量重验 + CSV 重建
│   │   ├── run-provider.sh            ← 单 Provider 执行器
│   │   └── score.js                   ← 评分逻辑（JS）
│   ├── providers/                     ← Provider 配置文件（harness 指令）
│   ├── tasks/                         ← 12 个任务 spec（中文）
│   ├── results/                       ← 评测产出
│   │   ├── eval-results.csv           ← 48 条完整数据（12 任务 × 4 Provider）
│   │   ├── README.md                  ← 评测总结（得分表 + 5 大核心发现）
│   │   ├── task-01-validation.md ~ task-12-flaky-test.md  ← 12 篇逐任务分析
│   │   └── process-analysis-url-shortener.md               ← 过程评测三轮对比
│   └── promptfooconfig.yaml           ← Promptfoo 框架配置
├── workspaces/                        ← 所有工作区
│   ├── 01-book-api/                   ← Superpowers 全流程（L1 校验 + L2 分页排序）
│   ├── 02-book-api-bare/              ← 裸 Agent 对照
│   ├── 03-book-api-kit-review/        ← agent-harness-kit 评审
│   ├── 04-book-api-forge/             ← HarnessForge 试用
│   ├── url-shortener-bare/            ← 过程评测：裸 Agent（13 测试）
│   └── url-shortener-harness/         ← 过程评测：Harness 交互版（19 测试）
└── share/                             ← 团队分享文档
    ├── Part1-问题.md                   ← 问题背景 + 两则案例
    ├── Part2-方法.md                   ← 评测方法论
    ├── Part3-数据.md                   ← 48 条数据 + 10 大发现
    └── Part4-总结.md                   ← 综合结论
```

---

## 🎯 调研范围

| 维度 | 覆盖 |
|------|------|
| **量化评测** | 12 个 Spring Boot 任务 × 4 组 Provider = 48 条记录 |
| **过程评测** | URL 短链接服务三轮对比（裸Agent vs Harness自动 vs Harness交互） |
| **Provider** | Baseline(裸Agent)、Superpowers(v5.1.0)、Gstack、OpenSpec |
| **模型** | DeepSeek-V4-pro |
| **实验项目** | Spring Boot 3.4 + Java 21 + Maven |
| **评估维度** | 编译通过率、测试覆盖、任务断言完成度、代码质量、Token 效率、架构决策 |

---

## 🔑 核心结论

### 1. 量化评测：Gstack 最优，Superpowers = Baseline

| Provider | 总分 | 平均分 | 满分次数 |
|----------|:---:|:---:|:---:|
| **Gstack** | **1090** | **90.8** | 2 |
| OpenSpec | 1065 | 88.8 | 2 |
| Baseline（裸Agent） | 1045 | 87.1 | 2 |
| Superpowers | 1045 | 87.1 | 2 |

- Gstack 的角色分工（CEO → Engineer → QA → DevOps）比 Superpowers 的严格流程（brainstorming → plan → TDD → review）更有效
- Superpowers 流程最繁重但得分与裸 Agent 持平——**流程步骤多 ≠ 质量好**
- 任务难度越大，Harness 价值越高（L4 中 Gstack 在 N+1 修复上领先 Baseline 25 分）

### 2. 过程评测：禁止交互 = 剥夺 Harness 核心价值

URL 短链接三轮对比实验揭示：

- 裸Agent 和 Harness自动在核心算法上**完全一致**（Base62 自增、不去重）——Agent 的"自问自答"没有改变核心决策
- 用户 3 轮交互改变了架构：随机 7 位 SecureRandom、24h urlIndex 去重、完整访问日志 100 条、Clock 注入
- 交互集中在需求澄清阶段（前 20% 时间），80% 的编码工作不需要用户参与

### 3. Token 效率差异巨大

Superpowers 的总 token 消耗是 Gstack 的 1.5-2 倍，但得分不增。流程繁重 = 成本增加 ≠ 质量提升。

### 4. Agent 共性问题

- 倾向"加新代码"而非"改旧代码"（Flaky 测试四组全部失败于未移除 LocalDate.now()）
- 自动场景下架构决策趋同（缺乏外部信息输入时只能选"合理默认值"）

---

## 🚀 进度回顾

```
Day 1 (06/09) ✅ 工具深潜 + 实操跑通——8/8 任务完成，3 个工具试用，11 条踩坑清单
Day 2 (06/10) ✅ 评测基础设施——CRLF 修复、verify.sh 加固、L1+L2 全量 26 条数据
Day 3 (06/11) ✅ L3+L4 全量 + 过程评测 + 分享文档——48 条完整数据、12 篇任务分析、
                     URL 短链接三轮对比、Part1-4 分享文档
```

---

## 📚 参考资源

- [Awesome Harness Engineering (walkinglabs)](https://github.com/walkinglabs/awesome-harness-engineering)
- [Awesome Harness Engineering (ai-boost)](https://github.com/ai-boost/awesome-harness-engineering)
- [OpenAI: Harness Engineering Paper (Feb 2026)](https://cdn.openai.com/agent-harness-engineering.pdf)
- [LangChain: Deep Agents on Terminal-Bench](https://blog.langchain.dev/deep-agents-terminal-bench/)
- [Simon Willison: Superpowers Review](https://simonwillison.net/2025/Oct/10/superpowers/)
