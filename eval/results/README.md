# Harness Engineering 评测总结

## 评测设计

- **12 个 Spring Boot 任务**：L1(简单) → L2(中等) → L3(复杂) → L4(高级)
- **4 组 Provider**：裸 Agent(Baseline) vs Superpowers vs Gstack vs OpenSpec
- **得分范围 0-100**：编译 gate | 测试 25 | 任务定制 60 | 代码质量 15
- **模型**: DeepSeek-V4-pro (2026-06-10)

## 全局得分表

| # | 任务 | Baseline | Gstack | OpenSpec | Superpowers | 最高 |
|---|------|:---:|:---:|:---:|:---:|:---:|
| ① | 参数校验 | 95 | 95 | 95 | 95 | -- |
| ② | Actuator | 90 | 90 | 90 | 90 | -- |
| ③ | 分页排序 | 90 | 90 | **95** | 90 | OpenSpec |
| ④ | 注册验证 | 95 | 90 | **100** | 90 | OpenSpec |
| ⑤ | Redis缓存 | 80 | **90** | 80 | **90** | Gstack/Super |
| ⑥ | 限流 | 90 | 90 | 90 | **100** | Superpowers |
| ⑦ | JWT认证 | **100** | **100** | **100** | 95 | Baseline/Gstack/OpenSpec |
| ⑧ | 三层重构 | 100 | 100 | 100 | 100 | -- |
| ⑨ | 文件上传 | 95 | 95 | 95 | 95 | -- |
| ⑩ | N+1修复 | 65 | **90** | 80 | 65 | Gstack |
| ⑪ | 并发预订 | 75 | **85** | 75 | 75 | Gstack |
| ⑫ | Flaky测试 | 70 | 70 | 70 | 70 | -- |

### 各 Provider 总分

| Provider | 总分 | 平均分 | 满分次数 |
|----------|:---:|:---:|:---:|
| **Gstack** | **1090** | **90.8** | 2 |
| OpenSpec | 1065 | 88.8 | 2 |
| Baseline | 1045 | 87.1 | 2 |
| Superpowers | 1045 | 87.1 | 2 |

## 核心发现

### 1. Superpowers 没有比裸 Agent 更好
**Superpowers 总分 = Baseline 总分 = 1045**。严格的 brainstorming → plan → TDD → review 流程没有带来整体得分提升。在 12 个任务中，Superpowers 只在任务⑥限流上独秀（100 vs 90），但在任务⑦JWT认证上反被 Baseline 超越（95 vs 100）。

### 2. Gstack 是最优 Harness
Gstack 总分最高（1090），在 N+1 修复（+25）和并发预订（+10）上拉开差距。角色分工带来的**多视角覆盖同一问题**比 Superpowers 的**流程步骤多**更有效。

### 3. 任务难度越大，Harness 价值越高
- L1（①②）：四组同分，Harness 无价值
- L2（③④⑤⑥）：开始出现分化（90-100）
- L3（⑦⑧⑨）：四组再次趋同，任务描述足够详细则 Agent 差距缩小
- L4（⑩⑪⑫）：Gstack 在 N+1 和并发上大幅领先，Harness 价值最大

### 4. Token 效率差异巨大
Superpowers 的总 token 消耗是 Gstack 的 1.5-2 倍，但得分不增。流程繁重 = 成本增加 ≠ 质量提升。

### 5. 所有 Harness 都怕"全局重构"
任务⑫ Flaky 测试中，四组全部漏了移除 LocalDate.now()。Agent-based 系统倾向于"加新代码"而非"改旧代码"，这是一个共性问题。

## 每个任务的详细分析
参见 [task-*.md](.) 系列文件。
