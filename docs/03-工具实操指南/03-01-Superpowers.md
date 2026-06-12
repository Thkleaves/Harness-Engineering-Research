# 03-01 — Superpowers 实操指南

> **优先级**: ⭐⭐⭐⭐⭐ 最高优先（推荐首选工具）  
> **定位**: 全流程 Agent 开发方法论 + 技能框架  
> **作者**: Jesse Vincent (obra) @ Prime Radiant  
> **仓库**: [github.com/obra/superpowers](https://github.com/obra/superpowers)

---

## 3.1.1 概述

Superpowers 是一套**完整的 AI Agent 编码工作流方法论**，以 14 个可组合的"技能"（Skills）形式提供。它不是简单的插件，而是一套**开发流程标准**——强制 Agent 在写任何代码之前先进行设计思考、编写计划、创建测试，从而让输出变得可预测、可重复、可审查。

> *"Superpowers is a complete software development workflow for your coding agents, built on top of a set of composable 'skills' and some initial instructions that make sure your agent uses them."*  
> — Jesse Vincent

---

## 3.1.2 核心概念

### 工作流全景

```
                      using-superpowers (入口 — 每次交互自动触发)
                               │
                    ┌──────────┴──────────┐
                    ↓                     ↓
           brainstorming              writing-skills
           (需求澄清 → 设计文档)        (自定义技能开发)
                    │
                    ↓
              writing-plans
           (设计文档 → 可执行任务列表)
                    │
                    ↓
            using-git-worktrees
           (为每个功能创建隔离工作区)
                    │
          ┌─────────┼─────────┐
          ↓         ↓         ↓
    subagent-dev  parallel   executing-plans
     (每个任务     (并行执行    (逐步骤执行
     子Agent)      独立任务)     人工检查点)
          │         │         │
          └─────────┼─────────┘
                    ↓
          requesting-code-review
           (跨任务评审 — 问题分严重级别)
                    ↓
        receiving-code-review
           (结构化响应反馈)
                    ↓
    verification-before-completion
           (强制重新验证)
                    ↓
    finishing-a-development-branch
           (自动化验证 → Merge/PR → 清理)
```

### 14 个核心技能一览

| 技能 | 阶段 | 作用 |
|------|:----:|------|
| **`using-superpowers`** | 🔼 入口 | 每次任务前自动检查适用技能 |
| **`brainstorming`** | 📐 设计 | 苏格拉底式需求澄清（逐个提问、探索替代方案、输出设计文档） |
| **`writing-plans`** | 📋 计划 | 将设计拆分为 2-5 分钟的可执行任务（含文件路径、代码片段、验证步骤） |
| **`writing-skills`** | 🔧 元 | 定义你自己的可测试技能（约定优于配置） |
| **`using-git-worktrees`** | 🌿 隔离 | 为每个功能创建隔离的 Git Worktree 和分支 |
| **`subagent-driven-development`** | 🤖 开发 | 为每个任务派遣独立的子 Agent，两阶段评审 |
| **`dispatching-parallel-agents`** | ⚡ 并行 | 并发执行独立子任务 |
| **`executing-plans`** | 📝 执行 | 按步骤执行计划，每个步骤后设人工检查点 |
| **`test-driven-development`** | 🧪 TDD | 强制 RED→GREEN→REFACTOR 循环 |
| **`systematic-debugging`** | 🔍 调试 | 四阶段根因分析（追踪→模式→假设→修复） |
| **`requesting-code-review`** | 👀 评审 | 结构化的预审查清单，问题按严重级别阻止推进 |
| **`receiving-code-review`** | 💬 响应 | 技术性地响应反馈（不是表演性同意） |
| **`verification-before-completion`** | ✅ 验证 | 在声称"完成"前强制运行全新的验证命令 |
| **`finishing-a-development-branch`** | 🚀 交付 | 自动化测试验证 → Merge/PR 决策 → 清理 Worktree |

---

## 3.1.3 安装与配置

### 方式一：官方插件市场（推荐）

```bash
# 注册插件市场
/plugin marketplace add obra/superpowers-marketplace

# 安装
/plugin install superpowers@superpowers-marketplace

# 验证
/help
# 应看到: /superpowers:brainstorm, /superpowers:write-plan, /superpowers:execute-plan
```

### 方式二：Anthropic 官方市场

```bash
/plugin install superpowers@claude-plugins-official
```

### 方式三：手工安装

```bash
# 克隆仓库
git clone https://github.com/obra/superpowers.git .claude/skills/superpowers

# 配置 CLAUDE.md 添加:
# 我的技能位于 .claude/skills/superpowers 目录
```

### 验证安装

```bash
# 检查技能是否加载成功
/help
# 应该可以看到所有超级技能命令
```

---

## 3.1.4 实操流程演示

### 场景：为一个新功能实现完整的 Superpowers 流程

```bash
# Step 1: 开始一个功能
> I need to add user authentication with JWT

# Agent 会自动触发 using-superpowers
# → 检测到需要 brainstorming
# → 开始逐个提问澄清需求

# Step 2: 设计阶段 (Brainstorming)
# Agent 会逐个提问：
#   "Users need to sign up with email and password?"
#   "Do we need social login?"
#   "Should we use access + refresh tokens?"
#   "Where should we store tokens?"
#   "What's the password hashing strategy?"
# 在每个问题后等待你的回答

# Step 3: 编写计划
# Agent 自动调用 writing-plans，输出计划文件：
#   .claude/plans/auth-implementation.md
# 包含：
#   - Task 1: Add bcrypt and jsonwebtoken dependencies (2 min)
#   - Task 2: Create user model with password hashing (5 min)
#   - Task 3: Create auth service (login/register) (5 min)
#   - Task 4: Create auth middleware (3 min)
#   - Task 5: Create auth routes (3 min)
#   - ...

# Step 4: 工作区隔离
# Agent 自动创建 git worktree 并切换到新分支

# Step 5: 执行 (subagent-driven-development)
# 为每个 Task 派遣子 Agent：
#   Subagent 1: 实现密码哈希 → 测试 → Review
#   Subagent 2: 实现 auth service → 测试 → Review
#   ...

# Step 6: 完成
# Agent 验证所有测试通过 → 询问合并还是 PR
```

---

## 3.1.5 关键机制详解

### 子 Agent 两阶段评审

每个子 Agent 完成任务后，自动执行两阶段评审：

```
Phase 1: 规范符合性 (Spec Compliance)
  - 代码是否符合 design spec？
  - 所有功能点是否覆盖？
  - API 接口是否一致？

Phase 2: 代码质量 (Code Quality)
  - 错误处理是否完善？
  - 安全性考虑？
  - 性能/可维护性评估？
```

### 计划文件的典型结构

```markdown
# Plan: 实现 JWT 认证

## Task 1: 安装依赖 (2 min)
- 文件: package.json
- 操作: 添加 bcrypt, jsonwebtoken
- 验证: npm ls bcrypt jsonwebtoken

## Task 2: User Model (5 min)
- 文件: src/models/user.ts
- 操作: 添加 email, passwordHash, createdAt
- 方法: hashPassword(), validatePassword()
- 验证: npm test -- --grep "User model"

## Task 3: Auth Middleware (3 min)
- 文件: src/middleware/auth.ts
- 操作: verifyToken → attach user to req
- 验证: curl -H "Authorization: Bearer <token>" /api/me
```

### 评审严重级别

| 级别 | 含义 | 行动 |
|:----:|------|------|
| 🔴 **Critical** | 安全漏洞 / 数据丢失风险 | **必须**修复后才能继续 |
| 🟡 **Major** | 功能不完整 / API 不兼容 | **推荐**修复后再提交 |
| 🔵 **Minor** | 代码风格 / 可读性问题 | 可选修复 |

---

## 3.1.6 最佳实践

### DO ✅

1. **从小功能开始**：先在一个小功能上跑通全流程，再应用到复杂场景
2. **充分参与 Brainstorming**：这是最重要的环节——你提供的答案质量直接决定后续产出质量
3. **善用 Plan 审查**：在执行前审查 Plan，确保任务拆解合理
4. **使用 Parallel 模式**：对于独立任务（前端组件 + API 路由），启用并行执行
5. **自定义 Writing Skills**：针对团队的框架和规范编写自定义技能

### DON'T ❌

1. **不要跳过 Brainstorming**：跳过的代价是后期返工 3-5 倍的工时
2. **不要手动修改 Worktree 中的文件**：让 Agent 管理，保持工作区隔离
3. **不要忽略 Critical Review**：Superpowers 标记的 Critical 确实是 Critical
4. **不要一次安排过多子 Agent**：建议 3-5 个并行，超过会导致 token 浪费

### 效率对照

| 功能规模 | 无 Superpowers | 有 Superpowers | 提升 |
|----------|:--------------:|:--------------:|:----:|
| 小功能 (<1h) | ~1h | ~1.2h | -20% |
| 中功能 (2-4h) | ~4h | ~2.5h | **+38%** |
| 大功能 (>4h) | ~8h | ~3h | **+63%** |
| 测试覆盖 | ~60% | ~90% | **+50%** |
| 返工率 | 常见 | 显著减少 | 显著 |

> **注意**：小功能反而稍慢是因为流程开销（设计→计划→评审），中大型功能收益显著。

---

## 3.1.7 已知限制与应对

| 限制 | 说明 | 对策 |
|------|------|------|
| **学习曲线** | 14 个技能需要时间熟悉 | 分阶段启用，先核心再全部 |
| **小功能惩罚** | 简单修改也要过完整流程 | 可使用 `executing-plans` 跳过 brainstorming |
| **Token 开销** | 流程本身消耗 Token | 核心 Skill 仅 ~2K tokens，子 Agent 分离了开销 |
| **Claude Code 依赖** | 部分功能依赖 Claude Code 特性 | 但在 Codex/Cursor 上也基本可用 |

---

## 3.1.8 资源与扩展

### 官方资源
- GitHub: [github.com/obra/superpowers](https://github.com/obra/superpowers)
- Release Notes: [RELEASE-NOTES.md](https://github.com/obra/superpowers/blob/main/RELEASE-NOTES.md)

### 社区衍生版本

| 变体 | 说明 |
|------|------|
| [BryanHoo/superpowers-ccg](https://github.com/BryanHoo/superpowers-ccg) | 中文汉化版 + 多模型路由（Claude 编排 + Codex 后端 + Gemini 前端） |
| [abudhahir/supremepower](https://github.com/abudhahir/superpowers) | Active Workflow Engine，共享 JSON 状态，VS Code 侧边栏 |
| [obra/superpowers-chrome](https://github.com/obra/superpowers-chrome) | 浏览器控制（Chrome DevTools Protocol），零依赖 |

### 相关工具搭配
- **+ agent-harness-kit**: 补充深度评审、SQLite 追踪
- **+ HarnessForge**: 先用 Forge 初始化项目脚手架，再装 Superpowers

---

## 3.1.9 安装检查清单

```
□ 已注册插件市场 (/plugin marketplace add obra/superpowers-marketplace)
□ 已安装插件 (/plugin install superpowers@superpowers-marketplace)
□ /help 显示所有 superpowers 技能命令
□ 已在小项目中跑通 brainstorming → plan → execute 流程
□ 已了解 git worktree 工作原理
□ 团队成员已阅读本指南
```

