# Part 4：建议 —— 团队怎么落地

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


---

## 4.2 推荐叠加路径

```
Step 1: HarnessForge init ───────────────────── 零成本
  └─ 任何项目先 init，让项目 Agent Ready
  └─ 这一步不增加任何开发环节

Step 2: Superpowers 插件 ────────────── 按场景开启
  ├─ 新功能开发、需求模糊  → 全开（brainstorming + plan + TDD + review）
  ├─ 算法实现（限流/加密）  → 开 TDD + review，关 brainstorming
  ├─ 简单修 Bug、重构      → 关 brainstorming + plan，保留 review + verify
  └─ 简单 CRUD、配置变更   → 关掉 Harness，裸 Agent 直接写

Step 3: agent-harness-kit ─────────── 关键节点
  ├─ PR merge 前           → 跑 10 维评审
  ├─ 安全敏感功能          → 专项安全评审
  └─ 定期（每周）          → 架构 + 技术债务评审
```

---

## 4.3 分场景选择矩阵

| 场景 | 推荐 Harness | 时长预期 | 原因 |
|------|:----------:|:---:|------|
| 简单 CRUD / 配置变更 | 不开 Harness | 1-2 min | L1 数据：Harness = 纯 Token 浪费 |
| API 设计先行（新模块） | OpenSpec | 5-8 min | Spec 驱动在 API 设计上有系统性优势 |
| 算法实现（限流/加密/编码） | **Superpowers** | 10-15 min | TDD 在算法上最强，唯一满分 |
| 多组件协作（认证/上传） | **Gstack** | 8-12 min | 角色分工覆盖不同关注面 |
| 性能优化 / Bug 修复 | **Gstack** | 5-10 min | QA 角色推动"验证是否真的修好了" |
| 存量代码重构 | Baseline 即可 | 3-5 min | L3 数据：裸 Agent 一次做对 |
| 安全敏感功能 | kit 10 维 + 任意 Harness | +5 min | 安全是专项，不是流程能兜底 |
| 需求极度模糊的新功能 | **Superpowers 全开** | 15-20 min | brainstorming 追问是核心价值 |

---

## 4.4 不要做的事

| ❌ 不要 | ✅ 应该 |
|--------|--------|
| 所有任务都开 Superpowers 全流程 | 按任务复杂度选择性开启 |
| 指望 Harness 让 Agent 主动改旧代码 | 重构需求主动写清楚"替换 X 为 Y" |
| 在简单任务上用 brainstorming | 需求明确就直接动手 |
| 只看单次得分评价 Harness | 纳入文档产出、多轮稳定性等 Soft Value |
| 只用一个 Harness | 三个叠加，场景化使用 |

### 关于 Superpowers 的正确期待

Superpowers 在本次评测中得分与 Baseline 持平，但如果你的场景是——

```
需求模糊（"加个校验"）+ 质量要求高 + 长期维护 + 多轮迭代
```

——那么 brainstorming 的追问深度、spec/plan 的文档沉淀、TDD 的测试覆盖、review 的盲区发现，这些价值会在 **第 2 轮、第 3 轮、一个月后** 逐渐显现。一次评测测不出来，不等于不存在。

---

## 4.5 一句话总结

> **Harness 不能把烂模型变好，但能让同一个模型在正确的场景下做得更深、更稳、更可追溯。**
>
> 关键不是"用不用"，是"知道什么时候用哪个"。
>
> 我们的数据说：角色分工（Gstack）在大多数场景胜出，但 TDD（Superpowers）在算法类任务上无人能及。裸 Agent 永远是小任务的正确答案。把三个工具叠加、按场景切换，才是最优解。

---

> 相关资源：
> - 评测数据：`eval/results/eval-results.csv`
> - 逐任务分析：`eval/results/task-*.md`
> - 实操记录：`records/` 目录
> - Day 1 对照实验：`records/任务④-裸Agent对比记录.md`
