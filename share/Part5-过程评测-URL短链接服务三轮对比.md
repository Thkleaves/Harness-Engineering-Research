# Part 5：过程评测 —— 自动评测遗漏了什么（10 min）

---

## 5.1 为什么还要做过程评测

12 任务的量化评测告诉我们**哪个 Harness 分高**，但它有一个根本局限：

> Agent 被告知"不要使用 AskUserQuestion"——**直接剥夺了 Harness 的核心价值点：帮用户澄清需求。**

brainstorming 原本是 AI 提问 → **用户回答** → AI 调整方案。在自动评测中变成了 AI 自问自答——Agent 猜用户要什么，然后按自己猜的做。

所以我们补了一轮实验：**同一个项目，三轮对比**。

---

## 5.2 实验设计

| 轮次 | 名称 | CLAUDE.md | 用户参与 |
|:---:|------|-----------|:---:|
| A | 裸Agent | 3 行，直接干活 | 无 |
| B | Harness自动 | 四阶段融合，**禁止**提问 | 无 |
| C | **Harness交互** | 四阶段融合，**允许**提问并要求确认 | **3 轮决策** |

**项目**：从零构建 URL 短链接服务（Spring Boot 3.4 + Java 21 + Maven）
**起始 Prompt**：`请从零构建一个URL短链接服务，支持创建短链接、302重定向和访问统计。`
**初始代码**：完全相同的空壳项目

---

## 5.3 基本指标

| 指标 | 裸Agent | Harness自动 | Harness交互 |
|------|:---:|:---:|:---:|
| 耗时 | 131s | 175s | ~44min（含人工） |
| 周转数 | 25 | 43 | ~60+ |
| Java 文件 | 10 | 13 | **16** |
| 测试方法 | 13 | 17 | **19** |
| Agent 提问 | 0 | 0（被禁止） | **3 轮** |
| 编译+测试 | ✅ | ✅ | ✅ |

---

## 5.4 架构决策对比

| 决策 | 裸Agent | Harness自动 | Harness交互 |
|------|:---:|:---:|:---:|
| **短码算法** | Base62 自增ID | Base62 自增ID | **随机 7 位 SecureRandom** |
| **幂等策略** | 无 | 无 | **24h urlIndex 去重** |
| **统计粒度** | AtomicLong 计数 | timestamp+UA | **timestamp+Referer+UA，100 条** |
| **时钟注入** | 无 | 无 | **Clock 注入（可测时间推进）** |
| **短码生成器** | 内嵌方法 | 独立类 | **接口抽象 + 实现类** |

**核心发现：自动 Harness 和裸 Agent 的核心算法选择完全一致（都是 Base62 自增）。只有用户交互改变了这个选择。**

---

## 5.5 用户的 3 轮交互如何改变了代码

| 轮次 | Agent 提问 | 用户回答 | 驱动的实现 |
|:--:|------|------|------|
| 1 | 短码策略三选一 | **"随机+24h 幂等"** | `SecureRandom` + `urlIndex` + `DEDUP_WINDOW` |
| 2 | 存储/统计/过期 | **"完整记录 100 条+永久有效"** | `AccessRecord(time, referer, ua)` + `Deque` |
| 3 | API 设计确认 | **"可以，TDD 流程"** | 严格红-绿-重构，19 测试 |

之后 Agent 自动完成 TDD → 实现 → 自审 → 修复 bug（自己发现 accessCount 计算错误）→ 最终验证。**用户只参与了前 20% 的需求澄清，80% 的编码工作不需要参与。**

---

## 5.4 关键发现

### 发现八：禁止用户交互的自动评测衡量的是"猜测能力"而非"协作能力"

```
自动 Harness 的"自问自答"：
  Q: "短码用 Hash、随机、还是自增？"
  A: "自增吧，简单无碰撞。"  ← Agent 猜了一个合理默认值

交互 Harness 的真实对话：
  Q: "短码用 Hash、随机、还是自增？"
  A: "随机，但要 24h 内同一 URL 返回已有短码。"  ← 用户给了真实需求
  
  → 架构完全不同了: 随机 SecureRandom + urlIndex 去重 + 碰撞重试
```

**自动评测中，Harness 被要求"自行决策"——这相当于把它的核心价值点（帮用户澄清需求并做对决策）关掉了。**

### 发现九：Harness 80% 的增量价值在前 20% 的时间

```
用户参与阶段（前 10 turns）:  需求澄清 + 方案选型 + API 设计确认
Agent 自动阶段（后 50 turns）: TDD → 实现 → 自审 bug → 修复 → 验证 ✅
```

用户不需要参与编码或 debug。Agent 自己发现并修复了 accessCount 的 bug 和 ShortCodeGenerator 接口化的问题。

### 发现十：三个"没有人能猜到"的决策

如果用户不参与交互，以下三个决策 Agent 永远猜不到：

1. **随机短码而非自增** — Agent 默认选最高效的方案，但用户可能因为安全考虑（不暴露内部 ID）选随机
2. **24h 幂等去重** — 这是一个业务决策，技术上看两种方案都合理，取决于产品需求
3. **完整访问日志而非仅计数** — Agent 默认"最小可用"，但用户可能需要后续数据分析

---

## 5.5 对评测框架的启示

| 自动评测能回答的 | 自动评测不能回答的 |
|----------------|-------------------|
| 哪个 Harness 测试写得最多 | Harness 能否帮用户做对决策 |
| 哪个 Harness 代码最模块化 | 有交互后哪个 Harness 体验最好 |
| 哪个 Harness Token 效率最高 | 用户参与后返工率是否下降 |

**建议**：评测框架中至少保留一个"有用户交互"的对照组。否则我们衡量的是"Agent 猜测需求的能力"，不是"Harness 引导协作的能力"。

---

## 5.6 过程数据溯源

| 数据 | 位置 |
|------|------|
| 裸Agent 源码 | `workspaces/url-shortener-bare/src/` |
| Harness交互源码 | `workspaces/url-shortener-harness/src/` |
| 裸Agent 会话日志 | `~/.claude/projects/d--...-url-shortener-bare/69aceaf1-*.jsonl` |
| Harness自动 会话日志 | `~/.claude/projects/d--...-url-shortener-harness/fb3921e4-*.jsonl` |
| Harness交互 会话日志 | `~/.claude/projects/d--...-url-shortener-harness/56ca26e7-*.jsonl` |
| 完整分析文档 | `eval/results/process-analysis-url-shortener.md` |
