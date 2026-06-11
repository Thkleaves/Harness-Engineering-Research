# Harness Engineering：AI 编码的瓶颈已从模型转向工程化

> **团队技术分享** | 2026-06-11 | 基于 12 任务 × 4 组对照实验 × 48 条评测数据

---

## Part 1：问题 —— 模型能干 ≠ 次次干得好（5 min）

### 1.1 一个真实场景

```
你: "帮我给 Book API 加个参数校验，别让空数据进来。"
AI:  "好的。" → 加了 @NotBlank，完事。
```

**你漏了什么？**

- ISBN 格式没校验——`"abc"` 通过
- 价格没小数位控制——`9.999` 入库
- PUT 部分更新不校验——传入空字段直接覆盖
- 错误响应是 Tomcat 默认 HTML 页面——前端解析不了
- 没写测试——下次重构时静默退化

这六个字"别让空数据进来"，裸 Agent 只理解了字面意思——**别是 null**。而加上 Superpowers 的 brainstorming，它会用 6 轮苏格拉底式追问把这句话展开成：ISBN 格式、价格小数位、PUT 更新策略、错误响应格式、自定义注解选型。

**同一模型。同一需求。结果天差地别。**

### 1.2 核心论点

```
传统认知:  AI 编码质量 = f(模型能力)
实际情况:  AI 编码质量 = f(模型能力 × 工程化框架)
                    ↑                    ↑
               天花板由模型定        落地效果由 Harness 定
```

LangChain 在 Terminal-Bench 2.0 上的实验已经证实了这一点：

| 优化方式 | 得分提升 | ROI |
|---------|:------:|:---:|
| 升级模型（GPT-4 → Claude 4） | +5.2% | 低（模型升级贵） |
| Harness 优化（架构 + hook + skill） | **+13.7%** | 高（工程优化一次投入） |

**模型是引擎，Harness 是让它可控行驶的底盘。** 引擎再好，底盘松散的车也开不稳。

### 1.3 Harness Engineering 是什么

```
┌─────────────────────────────────────────────────┐
│               Harness Engineering               │
│                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │ 上下文管理 │  │ 流程引导  │  │ 质量护栏  │      │
│  │          │  │          │  │          │      │
│  │ · 记忆   │  │ · 需求澄清│  │ · 安全扫描│      │
│  │ · 文档   │  │ · 实现计划│  │ · 测试强制│      │
│  │ · 项目认知│  │ · 代码审核│  │ · 编译验证│      │
│  └──────────┘  └──────────┘  └──────────┘      │
│                                                 │
│  目标: 让同一个模型在相同需求下产出更可靠的代码      │
└─────────────────────────────────────────────────┘
```

它不是"教模型写代码"——模型本来就会。它是**确保模型在正确的时机做正确的事**：先想清楚再动手、先写测试再实现、写完自证能跑。

---

## Part 2：方法 —— 我们怎么测的（10 min）

### 2.1 评测对象

| 组别 | 说明 | 核心特征 |
|:----:|------|----------|
| **A. Baseline** | 裸 DeepSeek V4 Pro，无任何 Skill | 直接写代码，不做额外步骤 |
| **B. Superpowers** | 注入 14 个技能 | brainstorming → plan → TDD → review → verify |
| **C. Gstack** | 注入角色分工技能 | /ceo 定需求 → /engineer 实现 → /qa 测试 → /devops 配置 |
| **D. OpenSpec** | 注入 Spec 驱动工作流 | 先写 Spec 定义 API → 再按 Spec 实现 |

**所有组使用相同模型（DeepSeek-V4-pro），相同任务 prompt，相同工作区代码基线。** 唯一的变量是 Harness/技能注入策略。

### 2.2 12 个 Spring Boot 任务

```
L1 简单 (1-2步)   ① 参数校验          ② Actuator 端点
                        基础技能调用              API 结构理解

L2 中等 (3-5步)   ③ 分页排序          ④ 注册+邮箱验证
                   ⑤ Redis 缓存        ⑥ Token Bucket 限流

L3 复杂 (5-10步)  ⑦ JWT 认证          ⑧ 三层重构
                   ⑨ 文件上传+异步      ⑩ N+1 查询修复

L4 高级 (10+步)   ⑪ 并发预订+乐观锁    ⑫ 时区 Flaky 测试
```

每个任务都是一个独立的 Spring Boot 项目，Agent 需要在已有代码基础上增量开发。

### 2.3 评分体系

```
Functional    40%  ← 编译 + 测试通过率（核心质量）
Quality       25%  ← 代码结构 + 错误处理 + 设计模式
Test Quality  15%  ← 测试存在？覆盖率？边界覆盖？
Process       10%  ← Skill 调用 + 步骤合理性
Cost          10%  ← Token 消耗（越低越高）
Security      Gate ← 一票否决
```

每条评测记录包含：得分、编译状态、测试通过数、Token 输入/输出、运行耗时。

### 2.4 不是"一次对话跑完"——是自动化评测

```
每个任务 → 4 个 Provider 独立运行 → 每次运行后自动执行 mvn compile + mvn test
→ verify.sh 解析 surefire 报告 → 输出 JSON 评分 → 汇总到 CSV
```

共 48 条有效评测记录。所有数据在 `eval/results/eval-results.csv`。

---

## Part 3：数据 —— 结果出人意料（15 min）

### 3.1 全局得分

| # | 任务 | Baseline | Gstack | OpenSpec | Superpowers | 最高 |
|---|------|:---:|:---:|:---:|:---:|:---:|
| ① | 参数校验 | 95 | 95 | 95 | 95 | — |
| ② | Actuator | 90 | 90 | 90 | 90 | — |
| ③ | 分页排序 | 90 | 90 | **95** | 90 | OpenSpec |
| ④ | 注册验证 | 95 | 90 | **100** | 90 | OpenSpec |
| ⑤ | Redis 缓存 | 80 | **90** | 80 | **90** | Gstack/Super |
| ⑥ | 限流 | 90 | 90 | 90 | **100** | Superpowers |
| ⑦ | JWT 认证 | **100** | **100** | **100** | 95 | 3 组并列 |
| ⑧ | 三层重构 | 100 | 100 | 100 | 100 | — |
| ⑨ | 文件上传 | 95 | 95 | 95 | 95 | — |
| ⑩ | N+1 修复 | 65 | **90** | 80 | 65 | Gstack |
| ⑪ | 并发预订 | 75 | **85** | 75 | 75 | Gstack |
| ⑫ | Flaky 测试 | 70 | 70 | 70 | 70 | — |

### 3.2 总分排名

| 排名 | Provider | 总分 | 平均分 | 满分 | 特征 |
|:----:|----------|:---:|:---:|:---:|------|
| 🥇 | **Gstack** | **1090** | **90.8** | 2 | 角色分工 × 轻量流程 |
| 🥈 | OpenSpec | 1065 | 88.8 | 2 | Spec 驱动 × API 设计优势 |
| 🥉 | Baseline | 1045 | 87.1 | 2 | 裸 Agent，简单直接 |
| 4 | Superpowers | 1045 | 87.1 | 2 | 流程最重，总分与裸 Agent 持平 |

> ⚠️ **Superpowers = Baseline = 1045 分。**
>
> 严谨的 brainstorming → plan → TDD → review → verify 全流程，在 12 个任务的综合评分上**没有超过裸 Agent**。

### 3.3 得分按难度分组

```
L1 (简单):  四组平分  → Harness 无价值
L2 (中等):  开始分化   → 90~100 之间波动
L3 (复杂):  再次趋同   → 任务详细时 Harness 差距缩小
L4 (高级):  最大分化   → Gstack 在 N+1(+25) 和并发(+10) 上大幅领先
```

**核心规律：任务越难、需求越模糊，Harness 的价值越大。但价值来自"多视角覆盖"，不是"多步骤流程"。**

### 3.4 Token 效率对比

| Provider | 总 Token (入+出) | 总耗时 | 平均分 | 千 Token 得分 |
|----------|:---:|:---:|:---:|:---:|
| Gstack | 639K | 4,953s | 90.8 | 1.70 |
| OpenSpec | 625K | 5,565s | 88.8 | 1.70 |
| Baseline | 687K | 5,758s | 87.1 | 1.52 |
| Superpowers | **991K** | **7,558s** | 87.1 | **1.05** |

**Superpowers 消耗了最多的 Token（+44% vs Gstack）和最长的耗时（+53% vs Gstack），但得分与 Baseline 持平。**

### 3.5 七个关键发现

#### 发现一：流程步骤多 ≠ 质量好

Superpowers 的 14 技能全流程没有转化为更高的得分。在 12 个任务中：
- 领先 1 次（⑥ 限流，100 分）
- 持平 6 次
- **落后 5 次**（③④⑦⑩⑪）

繁重流程有时反而让模型注意力分散——在 JWT 认证任务上，Superpowers 是**唯一丢分的组**（95 vs 其他三组 100），漏了全局异常处理。

#### 发现二：角色分工 > 步骤流程

Gstack 的 /ceo + /engineer + /qa + /devops 角色链在 12 个任务中 7 次领先或并列第一。关键机制不是"做更多步骤"，而是**同一问题被不同角色视角覆盖**：

```
Superpowers: 一个人按流程做 5 步 → 每一步还是同一个人的视角
Gstack:      4 个角色从不同角度审查同一个问题 → 盲区互补
```

N+1 修复是最好的例子：Gstack 90 vs Superpowers 65。QA 角色推动了 SQL 次数验证——这是其他所有组都漏的关键检查。

#### 发现三：Harness 改变的是"深度"不是"方向"

在 Day 1 的对照实验中，Superpowers 和裸 Agent 在架构选择上完全一致（都用 Jakarta Bean Validation 直接注解实体）。这个层级的判断来自模型本身。

差异在细节：

| 维度 | 裸 Agent | Superpowers |
|------|----------|-------------|
| title/author | `@NotBlank` | `@NotBlank` |
| isbn | `@NotBlank`（无格式校验） | `@NotBlank` + 自定义 `@ISBN`（去连字符 → 10/13 位） |
| price | `@NotNull` + `@Positive` | `@NotNull` + `@Positive` + 自定义 `@PriceFormat`（≤2 位小数） |
| 异常处理 | 无（依赖默认 400 页面） | GlobalExceptionHandler → 结构化 JSON |
| PUT 更新 | 零校验透传 | 注入 Validator 逐字段部分校验 |
| 测试 | 0 | 5 个手动 curl |
| 文档 | 0 | spec + plan + summary 3 个文件 |
| 范围控制 | 擅自加了 PathVariable/RequestParam 校验 | 严格按需求范围 |

**同一个模型，Harness 把它从"能跑"推到了"能交付"。**

#### 发现四：L1 任务上 Harness 是纯成本

参数校验和 Actuator 两个 L1 任务，四组完全同分。但 Superpowers 消耗了 2-3 倍的 Token 和时间。对于"需求明确、操作简单"的任务，brainstorming 追问是浪费时间。

#### 发现五：Harness 解决不了"全局重构畏难"

任务⑫ Flaky 测试修复——四组**全部 70 分**，全部漏了同一项：移除 `LocalDate.now()`。所有组都正确引入了 Clock 注入，但没有一组敢做全局替换。

Agent 的天性是"加新代码"而非"改旧代码"。所有当前 Harness 的引导都未能打破这一点。这是 Harness Engineering 的下一个前沿。

#### 发现六：Superpowers 在"算法实现 + 并发安全"场景最强

任务⑥ 限流——Superpowers 唯一满分（100 分）。Token Bucket 算法涉及 ConcurrentHashMap、线程安全、HTTP 429、可配置参数——这些恰好是 TDD（先写测试再写算法）的最佳场景。TDD 在"代码正确性可被测试明确验证"的任务上价值最大。

#### 发现七：Spec/Plan 文档是 Harness 最确定的差异化产出

不管 Superpowers 这一轮得分高低，它一定会在磁盘上留下：
- `specs/YYYY-MM-DD-<topic>-design.md`
- `plans/YYYY-MM-DD-<topic>-plan.md`

这些文档是**跨会话的记忆**。当上下文清空、下一个 session 开始时，裸 Agent 要重新理解项目，Superpowers 可以直接读设计文档。这在本次评测的 1-run 设计中没有体现——但如果每个任务跑 2-3 次，第二轮开始 Superpowers 的优势会显现。

---

## Part 4：建议 —— 团队怎么用（5 min）

### 4.1 三个工具的定位（不是竞品，是互补）

```
HarnessForge     → "3 秒让项目 Agent Ready"
  ├─ 生成 .claude/ 配置骨架（~20 个文件）
  ├─ 项目规范、安全策略、评审模板
  └─ 输出: 一个"能接住 Agent"的项目

Superpowers     → "完整开发流程"
  ├─ brainstorming → plan → TDD → review → verify
  ├─ 适用: 需求模糊、质量要求高的功能开发
  └─ 输出: 有 spec 有测试有 review 的交付物

agent-harness-kit → "生产级安全护栏"
  ├─ 10 维度评审（安全/性能/可靠性/架构...）
  ├─ Failures → Rules 自动转化
  └─ 适用: 关键 PR 的深度评审
```

### 4.2 推荐叠加路径

```
Step 1: HarnessForge init
  └─ 先把项目变成 Agent Ready
  └─ 成本和风险都最低

Step 2: Superpowers 插件（按场景开启）
  ├─ 新功能开发、需求模糊 → 开 brainstorming + plan + TDD
  ├─ 简单修 Bug、重构 → 关掉 brainstorming，只保留 review + verify
  └─ 关键: 不要无脑全开

Step 3: agent-harness-kit（关键节点）
  └─ PR merge 前、安全审计时跑 10 维评审
  └─ 频率低、价值高
```

### 4.3 分场景选择矩阵

| 场景 | 推荐 Harness | 原因 |
|------|:----------:|------|
| 简单 CRUD / 配置变更 | 关掉 Harness | L1 数据：Harness = 纯开销 |
| 算法实现（限流/加密/编码） | Superpowers | TDD 在算法类任务上最强 |
| 多组件协作（认证/文件上传/消息） | Gstack | 角色分工覆盖不同关注面 |
| API 设计先行 | OpenSpec | Spec 驱动在 API 设计上优势明显 |
| 性能优化 / Bug 修复 | Gstack | QA 角色推动"验证是否真的修好了" |
| 存量代码重构 | Baseline 即可 | L3 数据：裸 Agent 一次做对 |
| 安全敏感功能 | kit 10 维 + 任意 Harness | 安全是专项，不是流程能兜底的 |

### 4.4 不要做的事

1. **不要在所有任务上开 Superpowers 全流程** → Token 浪费 +44%，得分不增
2. **不要期待 Harness 解决"全局重构畏难"** → 当前所有 Harness 都推不动 Agent 改旧代码
3. **不要在简单任务上用 brainstorming** → L1 任务上它就是纯成本
4. **不要只看得分** → 文档产出、跨会话记忆这些 Soft Value 在单次评分中不体现

### 4.5 团队落地路线

```
Week 1: HarnessForge init → 选 1-2 个项目试点
Week 2: 仅在"需求模糊的新功能"上开 Superpowers
Week 3: 在关键 PR 上试用 kit 深度评审
Week 4: 收集数据 → 每个人记录"Harness 帮我发现了什么"
Month 2: 团队回顾 → 形成自己的"何时用哪个"内部指南
```

---

---

## Part 5：过程评测 —— 自动评测遗漏了什么（10 min）

> 详见 [Part5-过程评测-URL短链接服务三轮对比.md](Part5-过程评测-URL短链接服务三轮对比.md)

### 5.1 为什么还要做过程评测

12 任务评测中，Agent 被告知"不要使用 AskUserQuestion"——**这直接剥夺了 Harness 的核心价值：帮用户澄清需求。**

我们补了一轮实验：同一个 URL 短链接服务项目，三轮对比——裸Agent vs Harness自动（禁止提问） vs Harness交互（允许提问）。

### 5.2 核心发现

**自动 Harness 和裸 Agent 的核心算法选择完全一致（Base62 自增 ID）**。Agent 的"自问自答"在没有外部信息时只能选"合理的默认值"。只有用户的 3 轮交互改变了架构：随机短码（而非自增）、24h 幂等去重、完整访问日志。

| 决策 | 裸Agent | Harness自动 | Harness交互 |
|------|:---:|:---:|:---:|
| 短码算法 | Base62 自增 | Base62 自增 | **随机7位 SecureRandom** |
| 幂等策略 | 无 | 无 | **24h 去重窗口** |
| 统计粒度 | 计数 | timestamp+UA | **timestamp+Referer+UA** |
| 时钟注入 | 无 | 无 | ✅ |
| Agent 提问 | 0次 | 0次（被禁止） | **3轮** |
| 测试数 | 13 | 17 | **19** |

### 5.3 结论

**Harness 80% 的增量价值在前 20% 的时间。** 用户只需参与需求澄清阶段（3 轮决策），之后 Agent 自动完成 TDD → 实现 → 自审 bug → 修复 → 验证。用户不需要参与编码或 debug。

**自动评测衡量的是"Agent 猜测需求的能力"，不是"Harness 引导协作的能力"。**

---

## 附录：数据来源

- **评测配置**: `eval/promptfooconfig.yaml`
- **12 任务评测结果**: `eval/results/eval-results.csv`（48 条记录）
- **逐任务分析**: `eval/results/task-*.md`（12 个文件）
- **过程评测分析**: `eval/results/process-analysis-url-shortener.md`
- **Day 1 对照实验**: `records/任务④-裸Agent对比记录.md`
- **Superpowers 实操**: `records/任务③-Superpowers全流程总结.md`

所有评测使用 **DeepSeek-V4-pro** 模型，运行日期 2026-06-10 ~ 2026-06-11。
