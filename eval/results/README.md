# Harness Engineering 评测总结

## 评测设计

### 量化评测（12 任务）

- **12 个 Spring Boot 任务**：L1(简单) → L2(中等) → L3(复杂) → L4(高级)
- **5 组 Provider**：裸 Agent(Baseline) vs Superpowers vs Gstack vs OpenSpec vs wow-harness
- **得分范围 0-100**：编译 gate | 测试 25 | 任务定制 60 | 代码质量 15
- **模型**: DeepSeek-V4-pro（2026-06-10 ~ 2026-06-15）

### 过程评测（URL 短链接服务）

- **三轮对比**：裸Agent vs Harness自动（禁止交互）vs Harness交互（允许提问）
- **目标**：验证"禁止用户交互的自动评测是否遗漏了 Harness 的核心价值"
- **模型**: DeepSeek-V4-pro（2026-06-11）
- 详见 [process-analysis-url-shortener.md](process-analysis-url-shortener.md)

---

## 一、量化评测结果

### 全局得分表

| # | 任务 | Baseline | Gstack | OpenSpec | Superpowers | **wow-harness** | 最高 |
|---|------|:---:|:---:|:---:|:---:|:---:|:---:|
| ① | 参数校验 | 95 | 95 | 95 | 95 | 95 | -- |
| ② | Actuator | 90 | 90 | 90 | 90 | 90 | -- |
| ③ | 分页排序 | 90 | 90 | **95** | 90 | **95** | OpenSpec/wow |
| ④ | 注册验证 | 95 | 90 | **100** | 90 | 95 | OpenSpec |
| ⑤ | Redis缓存 | 80 | **90** | 80 | **90** | **90** | Gstack/Super/wow |
| ⑥ | 限流 | 90 | 90 | 90 | **100** | 90 | Superpowers |
| ⑦ | JWT认证 | **100** | **100** | **100** | 95 | **100** | Baseline/Gstack/OpenSpec/wow |
| ⑧ | 三层重构 | 100 | 100 | 100 | 100 | 100 | -- |
| ⑨ | 文件上传 | 95 | 95 | 95 | 95 | 95 | -- |
| ⑩ | N+1修复 | 65 | **90** | 80 | 65 | **80** | Gstack |
| ⑪ | 并发预订 | 75 | **85** | 75 | 75 | **85** | Gstack/wow |
| ⑫ | Flaky测试 | 70 | 70 | 70 | 70 | 70 | -- |

### 各 Provider 总分

| Provider | 总分 | 平均分 | 满分次数 |
|----------|:---:|:---:|:---:|
| **Gstack** | **1090** | **90.8** | 2 |
| **wow-harness** | **1085** | **90.4** | 2 |
| OpenSpec | 1065 | 88.8 | 2 |
| Baseline | 1045 | 87.1 | 2 |
| Superpowers | 1045 | 87.1 | 2 |

### 核心发现

#### 1. Superpowers 没有比裸 Agent 更好——软约束的天然上限
**Superpowers 总分 = Baseline 总分 = 1045**。后续检查会话日志发现根因：Agent 平均每任务只调用了 3-4 个 Superpowers 技能（14 个可选）。CLAUDE.md 写了"必须用 TDD""必须用 review"，但 Agent 自主选择跳过——13 个技能中只有 brainstorming 和 writing-plans 被用了 12/12 次，TDD 用了 ~8/12 次，code-review 只用了 ~3/12 次。**软约束的遵从率恰好是 wow-harness 文档所说的 ~20%。** Superpowers 只在任务⑥限流上调用了 6 个技能（满分 100），在任务⑦JWT 认证上跳过了 review（丢分 95 vs Baseline 100）。

#### 2. Gstack 是最优 Harness，wow-harness 紧追其后
Gstack 总分最高（1090），wow-harness 次之（1085），两者仅差 5 分。Gstack 在 N+1 修复（+25）和并发预订（+10）上拉开差距——角色分工带来的**多视角覆盖同一问题**比流程步骤更有效。wow-harness 靠 hooks 治理 + CLAUDE.md 行为约束在分页排序、Redis 缓存、N+1 修复上均比 Baseline 提升了 10-15 分。

#### 3. wow-harness 的硬约束是 Baseline 之上的增量
wow-harness 的 16 hooks + CLAUDE.md 治理框架 = 比裸 Agent 多 40 分。亮点：③分页排序 95（排序白名单，+5 vs Baseline）、⑤Redis 缓存 90（TTL 配置，+10）、⑩N+1 修复 80（SQL 验证，+15）。但硬约束不能替代"多视角思考"——⑩ 仍比 Gstack 低 10 分。

#### 4. 任务难度越大，Harness 价值越高
- L1（①②）：五组同分，Harness 无价值
- L2（③④⑤⑥）：开始出现分化（90-100）
- L3（⑦⑧⑨）：五组再次趋同，任务描述足够详细则 Agent 差距缩小
- L4（⑩⑪⑫）：Gstack 和 wow-harness 均有亮点，Harness 价值最大

#### 5. Token 效率差异巨大
Superpowers 的总 token 消耗是 Gstack 的 1.5-2 倍，但得分不增。流程繁重 = 成本增加 ≠ 质量提升。

#### 6. 所有 Harness 都怕"全局重构"
任务⑫ Flaky 测试中，五组全部漏了移除 LocalDate.now()。Agent-based 系统倾向于"加新代码"而非"改旧代码"，这是一个共性问题——硬约束也未能解决。

---

## 二、过程评测结果（URL 短链接服务）

见 [process-analysis-url-shortener.md](process-analysis-url-shortener.md)

### 三轮设计

| 轮次 | 名称 | CLAUDE.md | 用户参与 | 测试数 |
|:---:|------|-----------|:---:|:---:|
| A | 裸Agent | 3行，直接干活 | 无 | 13 |
| B | Harness自动 | 四阶段融合，禁止提问 | 无 | 17 |
| C | **Harness交互** | 四阶段融合，允许提问 | **3轮** | **19** |

### 关键对比

| 维度 | 裸Agent | Harness自动 | Harness交互 |
|------|:---:|:---:|:---:|
| 短码算法 | Base62 自增 | Base62 自增 | **随机7位 SecureRandom** |
| 幂等策略 | 无 | 无 | **24h urlIndex 去重** |
| 统计粒度 | AtomicLong 计数 | timestamp+UA | **timestamp+Referer+UA，100条** |
| 时钟注入 | 无 | 无 | ✅ |
| Agent提问 | 0次 | 0次（被禁止） | **3轮** |

### 核心发现

#### 1. 自动 Harness 改变的是代码质量，不是核心决策
Harness自动的代码更模块化、测试更多（17 vs 13），但在短码算法、幂等策略上与裸Agent**完全一致**（Base62自增、不去重）。Agent 的"自问自答"在没有外部信息时只能选择"合理默认值"，不能猜测用户的真实需求。

#### 2. 用户的 3 轮交互改变了架构
交互版的核心算法**完全不同**——随机7位（而非自增ID）、24h幂等（而非不去重）、完整访问日志（而非仅计数）。这些决策不是因为 Agent 更聪明，而是因为**你在前 10 个 turn 就告诉了它要什么**。

#### 3. Harness 80% 的价值在前 20% 的时间
交互集中在需求澄清阶段（3 轮提问），之后的 TDD/实现/Review 自动执行。用户只需参与决策，不需要参与编码或 debug——Agent 自己发现并修复了 accessCount bug 和 ShortCodeGenerator 接口化问题。

#### 4. 禁止用户交互的自动评测衡量的是"猜测能力"而非"协作能力"
12 任务评测中，Agent 被告知"不要使用 AskUserQuestion"——这直接剥夺了 Harness 的核心价值点（帮用户澄清需求并做对决策）。真实的 Harness 场景应该是**用户参与需求澄清 + Agent 自动执行实现**。

---

## 三、综合结论

### Harness 选型建议

| 场景 | 推荐 | 理由 |
|------|:---:|------|
| 需求明确、无歧义 | **裸Agent** | 最快、token最低、质量不差 |
| 多技术点组合 | **Gstack** | 多角色视角覆盖最全 |
| 需硬约束防偷懒 | **wow-harness** | hooks 机械执行 + 治理框架，比裸 Agent 多 40 分 |
| API 设计/状态机 | **OpenSpec** | Spec 驱动在接口定义上有优势 |
| 算法实现+测试密集 | **Superpowers** | TDD 流程提升测试覆盖率 |
| **需求模糊、需要决策** | **Harness + 用户交互** | 最大化 Harness 价值 |

### 关键教训

1. **不要禁止 AskUserQuestion**：这是 Harness 最核心的价值点
2. **流程步骤多 ≠ 质量好**：Superpowers 流程最长但得分=裸Agent
3. **角色分工 > 流程约束**：Gstack 证明多视角思考比严格流程更有效
4. **硬约束是软约束之上的增量**：wow-harness hooks 比 CLAUDE.md 单纯文本更可靠，但需环境兼容
5. **所有 Harness 都推不动"改旧代码"**：任务⑫ 五组同分 70，这是下一个前沿

---

## 各任务详细分析

- 12 任务得分分析：[task-01](task-01-validation.md) ~ [task-12](task-12-flaky-test.md)
- 过程对比分析：[process-analysis-url-shortener.md](process-analysis-url-shortener.md)
