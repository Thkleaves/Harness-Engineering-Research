# 03-02 — agent-harness-kit 实操指南

> **优先级**: ⭐⭐⭐⭐ 高（推荐作为 Superpowers 的补充）  
> **定位**: 生产级 Agent 基础设施（安全、治理、可观测性）  
> **仓库**: [npm: agent-harness-kit](https://www.npmjs.com/package/agent-harness-kit)  
> **版本**: v0.21.0 (2026.05)

---

## 3.2.1 概述

agent-harness-kit 是目前**功能最全面的开源 Harness Kit**，提供了 33 个技能、10 个评审子 Agent、6 种语言适配器、9 组 Hook 事件。相比 Superpowers 的"方法论"导向，agent-harness-kit 更偏向**基础设施**——它不定义你应该怎么做，而是提供一系列可插拔的"守卫"和"工具"来强制执行你的规则。

> *"Give hobby projects the patterns OpenAI used for 1M lines of agent-generated code."*  
> — agent-harness-kit 作者

---

## 3.2.2 核心特性

### 1. 33 个内置技能

| 技能 | 类别 | 说明 |
|------|:----:|------|
| `/add-feature` | 💡 开发 | 完整的 Plan→Dev→Test→Review 流程 |
| `/garbage-collection` | 🧹 维护 | 清理上下文碎片、过时规则、重复工具 |
| `/review-this-pr` | 👀 评审 | 当前分支的全面 PR 评审 |
| `/project-status` | 📊 度量 | 项目健康度仪表盘（测试覆盖率/债务/状态） |
| `/context-query` | 🔍 上下文 | 查询当前 Agent 上下文中的信息 |
| `/run-evals` | 🧪 评估 | 运行对抗性评估套件 |
| `/cost-report` | 💰 成本 | Token 使用报告和成本分析 |
| ... | ... | 共 33 个 |

### 2. 10 个评审子 Agent 维度

```
评审一个 PR 时，同时派遣 10 个专家子 Agent：

1. Advisor Agent       — 总体建议
2. Architecture Agent  — 架构一致性
3. Security Agent      — 安全漏洞
4. Reliability Agent   — 可靠性/错误处理
5. Performance Agent   — 性能影响
6. API Consistency     — API 一致性
7. Trace Failure Agent — 故障链路分析
8. Eval Rubric Agent   — 评估标准符合性
9. Adapter Compatibility — 语言适配器兼容性
10. Release Readiness  — 发布就绪度
```

### 3. 9 组 Hook 事件

```yaml
Hook 系统（覆盖全生命周期）:
  SessionStart      — Agent 启动时触发
  PreToolUse        — 工具使用前拦截 (可拒绝)
  PostToolUse       — 工具使用后审计
  PreToolCall       — 调用前拦截 (细粒度)
  PostToolCall      — 调用后记录
  Stop              — 停止时清理
  SubagentStop      — 子 Agent 停止时
  ContextOverflow   — 上下文超限时
  SessionEnd        — 会话结束时
```

### 4. 6 种语言适配器

| 适配器 | 功能 |
|--------|------|
| TypeScript | 强制模块边界、禁止循环依赖 |
| Python | 强制包结构、导入规范 |
| Go | GOPATH/LINT 规范 |
| Rust | Cargo 规范、unsafe 审查 |
| Swift | 模块化结构强制 |
| Kotlin | 包结构约束 |

---

## 3.2.3 安装与配置

### 作为 Claude Code 插件安装

```bash
# 通过 npm 安装
npm install -g agent-harness-kit

# 在项目中初始化
cd your-project
npx agent-harness-kit init

# 启动 Harness
npx agent-harness-kit start
```

### 项目结构（初始化后）

```
your-project/
├── .claude/
│   ├── harness/
│   │   ├── config.yaml         # 主配置
│   │   ├── hooks/              # Hook 脚本
│   │   ├── skills/             # 自定义技能
│   │   └── adapters/           # 语言适配器
│   ├── state/                  # SQLite 运行时状态
│   │   ├── features.db         # 功能追踪
│   │   ├── costs.db            # 成本追踪
│   │   └── failures.db         # 失败学习记录
│   ├── contracts/              # 任务合同
│   │   └── evidence/           # 验证证据包
│   └── CLAUDE.md
├── AGENTS.md
└── MEMORY.md
```

### 配置示例 (config.yaml)

```yaml
harness:
  version: "0.21.0"
  
  hooks:
    session-start: ["check-env", "load-contracts"]
    pre-tool-use: 
      - "validate-scope"      # 检查操作是否在范围内
      - "check-permissions"   # 权限检查
    post-tool-use:
      - "audit-log"           # 操作审计日志
    context-overflow:
      - "compress-context"    # 自动压缩上下文
      - "archive-decisions"   # 归档决策记录
  
  agents:
    review-dimensions:
      - advisor
      - architecture
      - security
      - reliability
      - performance
      - api-consistency
      - trace-failure
      - eval-rubric
      - adapter-compatibility
      - release-readiness
  
  cost-guardrails:
    max-per-session: 500000    # tokens
    max-per-task: 100000
    warn-at: 80%              # 百分比警告
  
  features:
    auto-gc: true             # 自动垃圾回收
    failure-to-rules: true    # 失败→规则记录
    evidence-bundles: true    # 证据包验证
```

---

## 3.2.4 实操流程演示

### 场景：使用 agent-harness-kit 添加新功能

```bash
# Step 1: 启动
npx agent-harness-kit start

# Step 2: 创建功能
/add-feature "Add pagination to user list API"

# agent-harness-kit 会自动：
# 1. 创建功能记录（SQLite）
# 2. 分配任务合同（Contract）
# 3. 指定语言适配器（如 TypeScript）
# 4. 启动开发子 Agent
```

### 运行评估

```bash
# 运行对抗性评估套件
/run-evals

# 生成项目健康报告
/project-status

# 检查成本
/cost-report

# 垃圾回收
/garbage-collection
```

### 错误学习机制

agent-harness-kit 有一个独特的 **Failures → Rules** 机制：

```
1. Agent 犯了一个错误（例如：修改了不应该碰的文件）
2. Hook 捕获到这个错误
3. 错误被记录到 failures.db
4. 系统分析错误模式 → 生成一条规则
5. 规则加入 Hook 检查列表
6. 后续类似操作被阻止
```

---

## 3.2.5 Superpowers vs agent-harness-kit 互补性

| 维度 | Superpowers | agent-harness-kit | 叠加效果 |
|------|:-----------:|:-----------------:|:--------:|
| 流程编排 | ✅ 完整的 14 步流程 | ⚠️ 基础流程 | Superpowers 提供流程，kit 提供守卫 |
| 工具治理 | ⚠️ 基础 | ✅ 9 Hook 组 + 权限 | 完整的"流程 + 治理" |
| 安全审查 | ⚠️ 基本代码审查 | ✅ 10 维度专业审查 | kit 审查作为 Superpowers Review 的补充 |
| 可观测性 | ⚠️ 日志级别 | ✅ SQLite + 成本追踪 | kit 提供 Superpowers 缺少的观测能力 |
| 错误学习 | ❌ | ✅ Failures→Rules | 补足 Superpowers 的空白 |
| Token 优化 | ✅ 子 Agent 分离 | ✅ 上下文压缩 | 双重优化 |

> **推荐组合方案**：
> - 日常开发用 **Superpowers** 的完整工作流
> - `/requesting-code-review` 时，额外跑 kit 的 10 维度评审
> - 定期跑 kit 的 `/garbage-collection` 做熵管理
> - kit 的 Hook 系统作为**安全兜底**

---

## 3.2.6 已知限制与应对

| 限制 | 说明 | 对策 |
|------|------|------|
| **配置复杂** | 初始化后需要手动调优 config.yaml | 从默认配置开始，逐步启用功能 |
| **Token 消耗高** | 10 维度评审开销大 | 仅在关键 PR 启用全部维度 |
| **版本年轻** | v0.21.0 仍可能有不稳定 | 先用非生产项目测试 |
| **中文资源少** | 文档和社区主要是英文 | 结合本指南使用 |
| **Go/Rust 适配器成熟度** | 部分语言适配器不如 TS 完善 | 以 TS/Python 适配器为主 |

---

## 3.2.7 安装检查清单

```
□ 已安装 agent-harness-kit (npm install -g agent-harness-kit)
□ 已在项目中初始化 (npx agent-harness-kit init)
□ config.yaml 已按需调整
□ 已了解 9 组 Hook 和各组的作用
□ 已在小项目中跑通 /add-feature 流程
□ 已尝试跑一次 /project-status
□ 团队成员已了解 "Failures → Rules" 机制
```

