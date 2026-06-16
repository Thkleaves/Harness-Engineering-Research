# Part 4：从三个 Harness 的 Prompt 源码学到的 8 条约束技巧

Superpowers 在 CLAUDE.md 里写了 14 个技能（brainstorming → plan → TDD → review → verify → finish），Agent 平均每任务只调用了 3-4 个——**遵从率 ~20%。** 同期评测的 wow-harness 得分更高。但它的"100% 遵从"并不适用于所有层面——hooks 是机械执行，skills 却和 Superpowers 一样依赖文本指令。**两者的有效覆盖面没有"20% vs 100%"那么悬殊。** 问题在于：约束到底该怎么写，Agent 才会遵守？

| 体系 | 核心理念 | 侧重 |
|------|---------|------|
| **wow-harness** | 机械约束 > 文本指令 | 动作强制，防跳过 |
| **agent-harness-kit** | 精确授权 > 开放工具 | 审计安全，防越权 |
| **Superpowers** | 流程链驱动，回答驱动需求 | 需求澄清，防做偏 |

但 wow-harness 的参与度在各层并不一致——**hooks 做到了 100% 机械执行，skills 和状态机却面临和 Superpowers 一样的问题：**

| 约束层 | 原设计规模 | 批量评测中实际生效 | 遵从率 |
|--------|:---:|:---:|:---:|
| **Hooks（机械拦截）** | 16 个 hooks | 仅保留 **5 个**（SessionStart×3 + SessionEnd×2 + PreCompact×1），11 个因兼容/干扰被禁用 | 保留的 100%，但原设计砍了 2/3 |
| **Skills（文本指令）** | 16 个专业化 skills | Agent 自主选择调用（和 Superpowers 同机制） | 推测 ~20%（与 Superpowers 同级） |
| **8 关状态机** | lead 技能 Gate 0→8 | **0%**——需要交互式 Gate 确认，批量 `claude -p` 模式下不可用 | 0% |

而 Superpowers 的技能调用数据（来源：`eval/results/README.md` 第 53 行）：

| 技能 | 12 任务中被调用次数 |
|------|:--:|
| brainstorming / writing-plans | 12/12 |
| TDD | ~8/12 |
| code-review | ~3/12 |
| verification | ~2/12 |
| 其余 9 个技能 | 0-1/12 |

**两种体系的技能层都依赖"Agent 自觉调用"——这是文本指令的天花板，不是某个工具的特有问题。**

---

## 1. 约束语法 — 列表无效，角色 + 前置条件有效

Superpowers 的 14 技能列表，Agent 只用 3-4——文本列表不足。wow-harness 的做法是给 Agent 定义角色的"不做什么"清单：

```markdown
## 角色（harness-dev/SKILL.md）
我是实现者。我的工作不是再决定"做什么"，而是把已冻结的判断变成可运行代码。
## 不做什么
- 不替 lead 或 arch 补边界决策
- 不把"编译过了"当成数据链路已通
```

```markdown
## 我是谁（lead/SKILL.md）
我是 fail-closed 的流程状态机。我的职责不是建议，而是**阻塞**。
LLM agent 在压力下会主动跳门。我的存在就是把"合理"挡在"合规"之后。
```

agent-harness-kit 用 allowed-tools + suggested-turns 从工具层面设边界：

```yaml
allowed-tools: Read, Edit, Write, Bash(npm run:*), Bash(pytest:*)
suggested-turns: 25
```

三条经验对应三条规则：

```
❌ "请按以下流程：1→2→3→4→5"    → 遵从率 ~20%
✅ "如果 X 没完成，不要开始 Y"      → 前置条件 > 步骤列表
✅ "完成后必须给 A、B、C"          → 输出契约 > 流程描述
```

> **日常用法**：CLAUDE.md 开头不写"请按流程"，写"你是 X，只做 Y，遇到 Z 停止"。把"应该做"改为"如果 X 没完成，不要做 Y"。

---

## 2. 需求澄清 — 一次只问一个选择，假设必须标注

Superpowers 的 brainstorming 在这点上既有正面经验，也有反面教训。

**正面**：Agent 在 6 轮提问中每次只问一个选择题（出自 `docs/records/任务③-Superpowers全流程总结.md`）：

| # | 问题 | 选项 |
|---|------|------|
| 1 | 哪些字段必填？ | title、author、isbn、price |
| 2 | "不为空"的粒度？ | 非 null + 去空格后不为空 |
| 3 | price 额外约束？ | >0 + 最多两位小数 |
| 4 | isbn 格式？ | 去连字符后 10 或 13 位数字 |
| 5 | 错误响应格式？ | 结构化 JSON，一次返回全部 |
| 6 | PUT 更新行为？ | 部分更新，传入的字段校验 |

方案三选一（A 注解+全局处理器 / B 手动 Service / C DTO 分离），每个附带利弊。

**反面**：自动评测中 Agent 被禁止提问，brainstorming 退化为"自问自答"，核心架构和裸 Agent 一样。过程评测中允许提问后（`eval/results/process-analysis-url-shortener.md`），3 轮交互改变了架构决策（随机短码、24h 幂等、Clock 注入）。

```markdown
❌ "自行分析需求，不要提问" → 自问自答，选了默认值
✅ "每轮只问一个选择题（附选项+利弊）。无法提问时，
    标注 [ASSUMPTION] 到 spec，后续决策基于这些假设展开。"
```

这和 wow-harness `snapshot_at: commit sha` 是同一思路：**让不可验证的前提显式化、可追溯。**

> **日常用法**：给 Agent 选择题模板——"每轮只问一个，2-3 个选项含利弊"。无法提问时，用 `[ASSUMPTION]` 标注假设。

---

## 3. 角色与门禁 — fail-closed 状态机

wow-harness 的 `lead` 技能定义 9 关状态机，每关有 entry_condition，不满足就 BLOCKED：

```yaml
gate_pack:
  current_gate: N
  entry_satisfied: true/false
  blockers: [...]

transition(current_gate, artifact) -> next_gate | BLOCKED
- entry_condition 未满足 → BLOCKED，只输出 blockers，不输出建议
- Gate 2/4/6/8 审查门必须用 TeamCreate（独立上下文）
- Gate 7→8：代码 commit 和 LOG.md 必须同时存在
- Gate 8 BLOCK → 回退到对应 Gate 修复
```

快速通道：5 条全满足才可跳过（≤3 文件、无契约变更、无跨模块接缝、不影响语义、不引入新决策）。

Superpowers 把 14 技能写成列表而非状态机，是遵从率低的技术根因——列表可以被滑过，`entry_satisfied: false → BLOCKED` 是机械阻断。

> **日常用法**：重要任务拆成关口——"①确认需求 → ②写计划 → ③写代码 → ④跑测试 → ⑤回归验证"。每关设条件，不满足就停。

---

## 4. 变更分级 — 分类决定流程深度

wow-harness 三级分类决定走多少关：

```markdown
| 分类           | 定义                    | 最低门禁       |
|---------------|------------------------|---------------|
| policy        | 边界、权限、对外语义      | Gate 0→8 全走  |
| contract      | API、schema、事件        | Gate 0→8 + 消费方 |
| implementation| 单模块内部实现           | 可走快速通道    |
```

kit 也有三级：Tiny（/feature-intake → 直接做）/ Normal（+ /create-story）/ High-risk（+ /add-adr）。

Superpowers 的 14 技能每任务都启动是过度设计——L1 简单任务五组同分，流程是纯消耗；L4 复杂任务 Harness 价值才显现。

> **日常用法**：prompt 开头加分类——"implementation 级（只改 1 个文件）→ 直接做。contract 级（改 API）→ 先列消费方。"

---

## 5. 输出与证据 — 四种实现，各有用武之地

**wow-harness：Output Contract + LOG.md**

```markdown
## Output Contract（harness-dev/SKILL.md）
1. change propagation checklist — 改了哪些契约、消费方在哪、哪些已同步
2. implementation closure — 代码入口改了什么、引入什么风险
3. test closure — 跑了哪些测试、哪些没跑、为什么
```

LOG.md 是从一次具体事故（某次 PLAN 在 Gate 8 才发现 LOG 是事后编的）催生的硬规则：

```markdown
Gate 7 — 执行 + 日志
LOG.md 不是可选项，不得事后补写。代码已 commit 但 LOG.md 不存在 = BLOCKED。
```

**agent-harness-kit：Evidence bundle**

```markdown
### Feature: <id>
### Files changed: <list>
### Structural test: PASS|FAIL
### Smoke test: PASS|FAIL
### Evidence bundle: .harness/evidence/<feature_id>.json
```

**Superpowers：spec + plan 文档 + 内嵌代码片段**

Superpowers 的做法是不用证据收集，而是用 spec 的精确度防止做偏。每个 plan 文件写入完整代码块和验证命令：

```java
@PutMapping("/{id}")
public ResponseEntity<Book> update(@PathVariable Long id, @RequestBody Book book) {
    Set<ConstraintViolation<Book>> violations = new HashSet<>();
    if (book.getTitle() != null)
        violations.addAll(validator.validateProperty(book, "title"));
    if (book.getAuthor() != null)
        violations.addAll(validator.validateProperty(book, "author"));
    if (book.getIsbn() != null)
        violations.addAll(validator.validateProperty(book, "isbn"));
    if (book.getPrice() != null)
        violations.addAll(validator.validateProperty(book, "price"));
    if (!violations.isEmpty())
        throw new ConstraintViolationException(violations);
    return bookService.update(id, book).map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

配验证命令：`curl -X PUT ... -d '{"price":35.50}'` → Expected 200 OK。

**Superpowers 的 spec/plan 作为"跨会话记忆"的价值**：裸 Agent 换会话后需要重新理解项目，spec/plan 读一次就能接上——在持续迭代的场景中这个价值远超单次评测得分。

| 方式 | 代表 | 成本 | 何时用 |
|------|------|:----:|-------|
| Output Contract | wow-harness | 低 | 通用 |
| Evidence bundle | kit | 高 | 安全/PR |
| 代码模板+验证命令 | Superpowers | 中 | 重要功能跨会话 |

> **日常用法**：任务末尾加"完成后给：改了哪些文件、测试结果原文、风险"。plan 里写完整代码块而非描述——Agent 照着填充比从推导可靠。

---

## 6. 失败知识库 — 把重复错误编成不变量

wow-harness 的 `crystal-learn` 把 Agent 反复出现的失败编目成 INV，格式固定：模式定义 → 典型形状 → 检测信号 → 缓解动作（注入目标 skill）。

**INV-0 快照幻觉**（Agent 用旧代码快照做决策，仓库已漂移）：

```markdown
## 典型形状
- "我看到 module/foo.py 里有 Handler.process()" → 实际已改名
- 同一次会话多次引用"刚才看到的"而没有重新 Read

## 缓解动作
1. 跨 WP 引用必须在执行时刻重新 Read
2. 计划文档标注 snapshot_at: <commit sha>
3. 多 WP 并行时，第二个前必须 git status 对齐
```

其他：INV-1 波纹衰减（改源头漏下游）、INV-4 真相源分裂（同一事实多处以谁为准）、INV-7 无主接缝（共享接口没 owner 就是 BLOCKED）。

> **日常用法**：维护 PITFALLS.md，每次 Agent 犯重复错误加一条——"现象 → 原因 → 规则"。关键任务的 plan 写完整代码块而非文字描述（给模板比让人推导偏离空间小）。

---

## 7. Scope 与异常 — 硬上限 + 所有异常都停止

agent-harness-kit 每个 skill 声明 allowed-tools 和 set-turns——**不需要的工具不授予**：

```yaml
allowed-tools: Read, Edit, Write, Bash(npm run:*), Bash(pytest:*)
suggested-turns: 25
```

wow-harness 的 `guardian-fixer` 限定了文件维度的 scope + 异常处理表：

```markdown
## Scope 硬上限
- 超过 3 个代码文件 → 标 needs_plan，停止
- API/schema/event 契约变更 → 标 needs_plan，停止
- 需要 DB migration → 标 needs_plan，停止
- 碰禁止修改的文件（CLAUDE.md、SKILL.md、hooks）→ 立即停止

## 异常处理
| 场景               | 操作                         |
|-------------------|-----------------------------|
| Scope 超 3 文件    | 标 needs_plan，停止           |
| 审查 2 轮不通过    | 标 blocked，停止              |
| 测试失败且无法修   | TEST.md 记录原因，标 blocked   |
| 所有异常路径都停止。不要强行继续。 |
```

> **日常用法**：子任务明确"只允许操作 X、Y、Z 文件"。超出范围或无法解决的问题 → 停止并报告。Agent 在压力下会绕过去——要提前告诉它"这不是可以绕的"。

---

## 8. 审查隔离 — 写代码和审代码不能同一上下文

wow-harness 的审查门强制 TeamCreate（独立上下文），拒绝 Agent subagent（共享上下文）：

```markdown
✅ TeamCreate("review-{plan-id}-gate-2")  — 独立上下文，多视角
❌ Agent(subagent_type="...")             — 共享上下文，单视角，不合规
```

kit 要求每个 feature 指定独立审查者名单：

```markdown
### Reviewer subagents to invoke:
architecture-reviewer, security-reviewer (if auth/IO touched),
reliability-reviewer (if retries/timeouts touched)
```

Superpowers 的 14 技能中也有 `requesting-code-review` 和 `receiving-code-review`，但问题是它们和其余 12 个技能平级，Agent 可以跳过——wow-harness 把审查做成独立门禁，不满足就 BLOCKED，这才是区别。

> **日常用法**：重要改动用独立的 Agent review。Prompt 只给 diff + 验收标准，不给"原来怎么设计的"——审查者知道设计意图就不是独立审查了。

---

## 三个核心经验

1. **Agent 会跳流程，但不一定会跳"和用户商量"**：Superpowers 的 14 技能遵从率 ~20%，但 brainstorming 单题追问在用户可用时有效改变了决策——问题不在于禁止提问，而在于**怎么问**。（对应 §2）

2. **Agent 用旧快照做新决策**：wow-harness 编成 INV-0（snapshot_at: commit sha），Superpowers 用 `[ASSUMPTION]` 标注假设是轻量替代。选哪个取决于你的成本偏好。（对应 §6）

3. **Agent 加新代码容易，改旧代码难**：任务⑫（Clock 注入替代 `LocalDate.now()`）五组全部 70 分。给"照着替换的模板"比给"自行实现的需求"可靠。（对应 §5）

---

> 完整源码见 `trials/url-shortener-wow/.claude/skills/`、`trials/01-book-api/docs/superpowers/plans/` 及 `scoring-experiment` 分支。
