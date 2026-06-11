# Part 3：数据 —— 7 个关键发现

---

## 3.1 全局得分表

| # | 任务 | Baseline | Gstack | OpenSpec | Superpowers | 最高 |
|---|------|:---:|:---:|:---:|:---:|:---:|
| ① | 参数校验 | 95 | 95 | 95 | 95 | — |
| ② | Actuator | 90 | 90 | 90 | 90 | — |
| ③ | 分页排序 | 90 | 90 | **95** | 90 | OpenSpec |
| ④ | 注册验证 | 95 | 90 | **100** | 90 | OpenSpec |
| ⑤ | Redis 缓存 | 80 | **90** | 80 | **90** | Gstack/Super |
| ⑥ | 限流 | 90 | 90 | 90 | **100** | Superpowers |
| ⑦ | JWT 认证 | **100** | **100** | **100** | 95 | 三组并列 |
| ⑧ | 三层重构 | 100 | 100 | 100 | 100 | — |
| ⑨ | 文件上传 | 95 | 95 | 95 | 95 | — |
| ⑩ | **N+1 修复** | **65** | **90** | 80 | **65** | Gstack |
| ⑪ | **并发预订** | 75 | **85** | 75 | 75 | Gstack |
| ⑫ | Flaky 测试 | 70 | 70 | 70 | 70 | — |

---

## 3.2 总分排名

| 排名 | Provider | 总分 | 平均分 | 满分次数 | 定性 |
|:----:|----------|:---:|:---:|:---:|------|
| 🥇 | **Gstack** | **1090** | **90.8** | 2 | 角色分工 × 轻量流程 |
| 🥈 | OpenSpec | 1065 | 88.8 | 2 | Spec 驱动 × API 设计优势 |
| 🥉 | Baseline | 1045 | 87.1 | 2 | 裸 Agent，简单直接 |
| 4 | Superpowers | 1045 | 87.1 | 2 | 流程最重，总分与裸 Agent 持平 |

> ⚠️ **Superpowers = Baseline = 1045 分。**
>
> 严谨的 brainstorming → plan → TDD → review → verify 全流程，在 12 个任务的**综合得分**上没有超过裸 Agent。

---

## 3.3 得分按难度分组

```
L1 (简单任务 ①②)
  ┌────────────────────────────┐
  │ 四组完全同分            │  95 / 90
  │ → Harness 没有带来任何差异  │
  └────────────────────────────┘

L2 (中等任务 ③④⑤⑥)
  ┌────────────────────────────┐
  │ 开始分化: 80~100          │
  │ → OpenSpec 在 ③④ 领先      │
  │ → Superpowers 在 ⑥ 唯一满分  │
  │ → Gstack 在 ⑤ 并列第一     │
  └────────────────────────────┘

L3 (复杂任务 ⑦⑧⑨)
  ┌────────────────────────────┐
  │ 重新趋同: 95~100          │
  │ → 任务描述足够详细时        │
  │ → 各组能力差距被需求细化抹平 │
  └────────────────────────────┘

L4 (高级任务 ⑩⑪⑫)
  ┌────────────────────────────┐
  │ 最大分化: 65~90           │
  │ → Gstack 在 ⑩ 领先 25 分    │
  │ → Gstack 在 ⑪ 领先 10 分    │
  │ → 四组在 ⑫ 全部 70 分      │
  └────────────────────────────┘
```

**规律：任务越难、需求越模糊，Harness 的价值越大。但价值来自"多视角覆盖"，不是"多步骤流程"。**

---

## 3.4 Token 效率对比

| Provider | 总 Token (入+出) | 总耗时 | 平均分 | 千 Token 得分 |
|----------|:---:|:---:|:---:|:---:|
| **Gstack** | **639K** | 4,953s | **90.8** | **1.70** |
| OpenSpec | 625K | 5,565s | 88.8 | 1.70 |
| Baseline | 687K | 5,758s | 87.1 | 1.52 |
| Superpowers | 991K | 7,558s | 87.1 | 1.05 |

**Superpowers 消耗了最多的 Token（+44% vs Gstack）和最长的耗时（+53% vs Gstack），得分与 Baseline 持平。千 Token 效率不到 Gstack 的 2/3。**

---

## 3.5 七个关键发现

---

### 发现一：流程步骤多 ≠ 质量好

Superpowers 的 14 技能全流程在 12 个任务中的表现：

| 表现 | 任务数 | 具体任务 |
|------|:---:|---------|
| 领先 | 1 | ⑥ 限流（100 分） |
| 持平 | 6 | ①②⑤⑧⑨⑫ |
| **落后** | **5** | ③④⑦⑩⑪ |

**繁重流程有时反而让模型注意力分散。** JWT 认证任务上，Superpowers 是**唯一丢分的组**（95 vs 其他三组 100）——漏了全局异常处理（@ExceptionHandler）。TDD 流程中过分关注功能测试而忽略了横切关注点。

---

### 发现二：角色分工 > 步骤流程

Gstack 在 12 个任务中 7 次领先或并列第一。关键机制：

```
Superpowers 模式: 一个人按流程做 5 步
  brainstorming → plan → TDD → review → verify
  → 每一步还是同一个人的视角，盲区一致

Gstack 模式:       4 个角色同时审视
  /ceo → /engineer → /qa → /devops
  → 不同角色有不同的关注面，盲区互补
```

**N+1 修复是最好的例子**：Gstack 90 vs Superpowers 65（差距 25 分）。

| 检查项 | Baseline | Gstack | OpenSpec | Superpowers |
|--------|:--:|:--:|:--:|:--:|
| JOIN FETCH / @EntityGraph | ✓ | ✓ | ✓ | ✓ |
| **SQL count 验证** | ✗ | **✓** | ✗ | ✗ |
| N+1 测试 | ✓ | ✓ | ✓ | ✓ |
| API response preserved | ✗ | ✓ | ✓ | ✗ |

Gstack 的 QA 角色推动用 StatementInspector 验证 SQL 实际次数——**其他三组全部漏了"验证是否真的优化了"这一关键步骤。**

---

### 发现三：Harness 改变的是"深度"不是"方向"

Day 1 对照实验的有力证据——Superpowers 和裸 Agent 在架构选择上完全一致：

| 决策 | 裸 Agent | Superpowers |
|------|:--:|:--:|
| 方案选择 | Jakarta Bean Validation 直接注解实体 | 同（三选一后决定） |
| 是否建 DTO | 否 | 否 |

架构判断来自模型本身。但细节差异显著：

| 维度 | 裸 Agent | Superpowers |
|------|----------|-------------|
| title / author | `@NotBlank` | `@NotBlank` |
| isbn | `@NotBlank`（无格式校验） | `@NotBlank` + 自定义 `@ISBN`（去连字符 → 10/13 位数字） |
| price | `@NotNull` + `@Positive` | `@NotNull` + `@Positive` + 自定义 `@PriceFormat`（≤2 位小数） |
| 异常处理 | 无（依赖 Tomcat 默认 400 页面） | GlobalExceptionHandler → 结构化 JSON |
| PUT 部分更新 | 零校验，原样透传 | 注入 Validator，逐字段校验非 null |
| 测试 | 0 个 | 5 个手动 curl 验证 |
| 文档 | 0 | spec + plan + summary 共 3 个文件 |
| 范围控制 | 擅自加了 PathVariable/RequestParam 校验 | 严格按需求边界 |

**同一个模型，Harness 把它从"能跑"推到了"能交付"。**

---

### 发现四：L1 任务上 Harness 是纯成本

| Provider | ① 参数校验 | Token | ② Actuator | Token |
|----------|:---:|:---:|:---:|:---:|
| Baseline | 95 | 70K | 90 | 117K |
| Superpowers | 95 | **109K** | 90 | 102K |
| Gstack | 95 | 54K | 90 | 72K |
| OpenSpec | 95 | 51K | 90 | 127K |

两个 L1 任务四组同分，但 Superpowers 消耗了显著更多的 Token 和时间。**对于需求明确、操作简单的任务，brainstorming 追问是浪费。**

---

### 发现五：所有 Harness 都怕"全局重构"

任务⑫ Flaky 测试修复——**四组全部 70 分**：

| 检查项 | Baseline | Superpowers | Gstack | OpenSpec |
|--------|:--:|:--:|:--:|:--:|
| Clock 注入 | ✓ | ✓ | ✓ | ✓ |
| Clock.fixed() in test | ✓ | ✓ | ✓ | ✓ |
| 时区处理 | ✓ | ✓ | ✓ | ✓ |
| 多时区参数化测试 | ✓ | ✓ | ✓ | ✓ |
| **移除 LocalDate.now()** | **✗** | **✗** | **✗** | **✗** |

所有组正确引入了 Clock 注入和固定时间测试，但**无一组彻底移除主代码中的 LocalDate.now() 调用**。Agent 的天性是"加新代码"而非"改旧代码"。所有当前 Harness 的引导都未能打破这一点。

**这是本轮评测唯一一组四组同分且漏同一项的任务——也是 Harness Engineering 的下一个前沿。**

---

### 发现六：Superpowers 在"算法 + 并发"场景最强

任务⑥ 限流——**Superpowers 唯一满分（100 分）**：

```
Baseline:    90 分 — 算法一次过，漏了 Retry-After 或并发测试边界
Gstack:      90 分 — 角色分工未带来额外覆盖
OpenSpec:    90 分 — Spec 未覆盖限流算法的实现细节
Superpowers: 100 分 — TDD 先写并发测试暴露竞态，再写算法
```

Token Bucket 算法涉及 ConcurrentHashMap、线程安全、HTTP 429、可配置参数——这些恰好是 TDD 的最佳场景。**TDD 在"代码正确性可被测试明确验证"的任务上价值最大。**

但代价也明显：928s vs Baseline 268s（3.5 倍时间），104K token vs Baseline 42.5K（2.4 倍）。

---

### 发现七：Spec/Plan 文档是 Superpowers 最确定的差异化产出

不管 Superpowers 这一轮得分高低，它一定在磁盘上留下：

```
docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md
docs/superpowers/plans/YYYY-MM-DD-<topic>-plan.md
```

这些文档的价值在本次评测的 1-run 设计中**没有体现**。但如果每个任务跑 2-3 次：

```
裸 Agent:
  Session 1: 写代码 → 得分 90
  Session 2 (清上下文): 重新理解项目 → 得分可能下降
  Session 3 (清上下文): 再次重新理解 → 一致性差

Superpowers:
  Session 1: brainstorming → spec → plan → 代码 → 得分 90
  Session 2 (清上下文): 读 spec.md → 立即理解项目 → 得分稳定
  Session 3 (清上下文): 读 spec.md + plan.md → 持续稳定
```

**Spec 和 Plan 是"跨会话的记忆"，而评测只测了第一次。** 这是 Superpowers 的 Soft Value，在单次评分中不体现，但在长期项目中持续产生价值。

---

## 3.6 汇总

```
               得分    Token效率   简单任务  复杂任务   文档产出   多轮稳定
Baseline      87.1     1.52      高效      中等      无        低
Superpowers   87.1     1.05      浪费      中-高    有        高（推测）
Gstack        90.8     1.70      高效      最高      中        高
OpenSpec      88.8     1.70      高效      高        高        高
```

评测数据表明：不同 Harness 在不同场景下各有优劣势，不存在一个在所有情况下都最优的方案。

---

## 3.7 过程评测：自动 Harness = 裸 Agent 的核心原因

以上 7 个发现来自 12 任务的**自动评测**（Agent 被禁止提问）。我们补了一轮**过程评测**来验证一个假设：**禁止交互是否就是 Superpowers 得分不高的根因？**

### 实验：URL 短链接服务三轮对比

| 轮次 | 名称 | 用户参与 | 测试数 | 短码算法 | 幂等策略 |
|:---:|------|:---:|:---:|------|------|
| A | 裸 Agent | 无 | 13 | Base62 自增 | 无 |
| B | Harness 自动 | 无 | 17 | Base62 自增 | 无 |
| C | **Harness 交互** | **3 轮** | **19** | **随机7位** | **24h 去重** |

**核心数据：A 和 B 的核心架构选择完全一致（Base62 自增、无幂等）。只有 C 的 3 轮用户交互改变了架构。**

### 发现八：禁止交互的自动评测衡量的是"猜测能力"而非"协作能力"

Harness 的 brainstorming 原本设计是 AI 提问 → **用户回答** → AI 调整。自动评测中退化为：

```
自动 Harness 自问自答:
  Q: "短码用 Hash、随机、还是自增？"
  A: "自增吧，简单无碰撞。"  ← 猜了一个"合理的默认值"
  → 代码结构更好、测试更多，但核心决策跟裸 Agent 一模一样

交互 Harness 真实对话:
  Q: "短码用 Hash、随机、还是自增？"
  A: "随机，但要 24h 内同一 URL 返回已有短码。"  ← 用户给了真实需求
  → 架构完全不同: SecureRandom + urlIndex 去重 + 碰撞重试 + Clock 注入
```

**自动评测相当于把 Harness 的核心价值点（帮用户澄清需求并做对决策）关掉了。**

### 发现九：Harness 80% 的增量价值在前 20% 的时间

```
用户参与阶段（前 10 turns）:  需求澄清 + 方案选型 + API 设计确认
Agent 自动阶段（后 50 turns）: TDD → 实现 → 自审 bug → 修复 → 验证 ✅
```

用户不需要参与编码或 debug。Agent 自己发现并修复了 accessCount 计算错误和 ShortCodeGenerator 接口化问题。

### 发现十：三个"没有人能猜到"的决策

如果用户不参与交互：

1. **随机短码而非自增** — Agent 默认最简方案，但用户可能因安全考虑选随机
2. **24h 幂等去重** — 业务决策，技术上两种都合理，Agent 无法预判
3. **完整访问日志而非仅计数** — Agent 默认"最小可用"，但用户需要后续分析

---

## 3.8 汇总

```
               得分    Token效率   简单任务  复杂任务   文档产出   多轮稳定   交互质量
Baseline      87.1     1.52      高效      中等      无        低        N/A
Superpowers   87.1     1.05      浪费      中-高    有        高（推测）  高（推测）
Gstack        90.8     1.70      高效      最高      中        高        中（推测）
OpenSpec      88.8     1.70      高效      高        高        高        中（推测）
```

评测数据表明：自动评测中禁止 Agent 提问，直接导致 Harness 的 brainstorming 退化为自问自答。过程评测补做的交互实验证实了这一点——允许提问后核心架构决策完全不同。

---

> 数据来源：`eval/results/eval-results.csv`（48 条记录）、`eval/results/task-*.md`（12 篇分析）、`eval/results/process-analysis-url-shortener.md`（过程评测）
