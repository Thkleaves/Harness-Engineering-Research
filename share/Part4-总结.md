# Part 4：总结 —— 评测数据与过程观察

---

## 4.1 三个工具的定位

```
HarnessForge          Superpowers            agent-harness-kit
───────────           ────────────           ────────────────
入口级                 流程级                 审计级

"3 秒让项目            "完整开发流程"          "生产级安全护栏"
 Agent Ready"

· ./claude/ 配置骨架   · brainstorming        · 10 维度评审
· 项目规范、安全策略    · writing-plans         · Failures → Rules
· 评审模板、Agent定义   · TDD + review         · Hook 系统
· ~20 个文件            · verify + finish      · 证据包、证明链

输出:                 输出:                  输出:
一个"能接住 Agent"    有 spec 有测试有        PR 评审报告 +
的项目                review 的交付物        安全漏洞清单
```

三个工具不是竞品关系，各自解决不同层面的问题：Forge 做项目初始化，Superpowers 做开发流程，kit 做安全审计。

---

## 4.2 三种工具的叠加关系

评测中 Gstack 总分最高（1090），但这不是说"只用 Gstack 就行"——Superpowers 在算法类任务（任务⑥ 限流 100 分）无人能及，OpenSpec 在 API 设计类任务（任务④ 满分）上最强。不同工具在不同场景下互补。

评测过程中实际采用的叠加方式：

1. **HarnessForge init** — 先把项目变成 Agent Ready，这步没有额外成本
2. **Superpowers 插件** — 评测中在需求模糊的任务上开了全流程，简单任务上只保留了 review + verify，关掉了 brainstorming
3. **agent-harness-kit** — 只在关键节点用（PR review、安全审计），频率低价值高

---

## 4.3 不同场景在评测中的表现

| 场景 | 评测中领先的 Harness | 数据 |
|------|:----------:|------|
| 简单 CRUD / 配置变更 | 各组同分 | L1 数据：四组 95/90，Harness 无差异 |
| 算法实现（限流/加密/编码） | **Superpowers** | 任务⑥ 唯一满分，TDD 在算法上最强 |
| 多组件协作（认证/文件上传/消息） | **Gstack** | 任务⑦⑨ 多角色覆盖不同关注面 |
| API 设计先行 | **OpenSpec** | 任务③④ 领先，Spec 驱动在接口定义上占优 |
| 性能优化 / Bug 修复 | **Gstack** | 任务⑩ 领先 25 分，QA 推动"验证是否真的修好" |
| 存量代码重构 | 各组同分 | L3 数据：四组 100，裸 Agent 一次做对 |
| 需求极度模糊的新功能 | 数据不足 | 过程评测表明，需要用户交互才能发挥 Harness 价值 |
| 全局重构（改旧代码） | 全部落后 | 任务⑫ 四组 70 分，所有 Harness 都推不动 Agent 改旧代码 |

![12任务 × 4 Provider 得分趋势 — 不同场景下各组差异显著](../eval/results/charts/02-score-trend.png)

---

### 关于 Superpowers 的得分

Superpowers 在本次评测中总分与 Baseline 持平（1045），但过程评测揭示了原因：自动评测中 Agent 被禁止提问，brainstorming 退化成了自问自答。补做的交互实验证明，允许用户参与后架构决策完全不同（随机码 vs 自增ID，有幂等 vs 无幂等）。

另外 Superpowers 的 spec/plan 文档产出是跨会话的记忆——单次评测测不出这个价值，但在多轮迭代和长期维护中会逐渐显现。

---

## 4.4 一句话总结

> **Harness 不能把烂模型变好，但能让同一个模型在正确的场景下做得更深、更稳、更可追溯。**
>
> 评测数据表明：Gstack 在大多数场景得分最高，但 Superpowers 在算法类任务上无人能及，OpenSpec 在 API 设计上有系统性优势。裸 Agent 永远是小任务的正确答案。三个工具叠加使用、按场景切换的方式，比单一 Harness 的效果更好。

---

> 相关资源：
> - 评测数据：`eval/results/eval-results.csv`（48 条记录）
> - 逐任务分析：`eval/results/task-*.md`（12 个任务）
> - 过程评测分析：`eval/results/process-analysis-url-shortener.md`
> - Day 1 对照实验：`records/任务④-裸Agent对比记录.md`
> - Superpowers 实操记录：`records/任务③-Superpowers全流程总结.md`
> - HarnessForge 试用：`records/任务⑥-HarnessForge试用.md`
> - Kit 10 维评审：`records/任务⑤-kit-10维度评审.md`
> - Book API 项目（workspace 01-04）：`workspaces/` 目录
> - URL 短链接服务：`workspaces/url-shortener-*/`
