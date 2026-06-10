# 03-03 — HarnessForge 实操指南

> **优先级**: ⭐⭐⭐ 中（推荐作为快速初始化的补充工具）  
> **定位**: 极速通用的 Harness 生成器  
> **仓库**: [PyPI: harnessforge](https://pypi.org/project/harnessforge/)  
> **版本**: v0.2.1 (2026.05)

---

## 3.3.1 概述

HarnessForge 是一个**模型无关的通用 Harness 生成器**——在任何项目目录中执行一条命令，3 秒内产生全套 Agent 配置文件（AGENTS.md、SOUL.md、TOOLS.md、MEMORY.md、SKILLS/、IDE 适配文件）。与其他工具不同，它不定义流程或方法论，而是解决一个很具体的问题：**"把这个目录变成 Agent Ready 状态"**。

> 类比：HarnessForge 之于 Agent 配置，就像 `create-react-app` 之于 React 项目。

---

## 3.3.2 核心特性

### 极致速度

```bash
# 在任意项目目录中执行
cd my-project
harnessforge init

# 输出:
# ✅ Generated AGENTS.md
# ✅ Generated SOUL.md
# ✅ Generated MEMORY.md
# ✅ Generated TOOLS.md
# ✅ Generated SKILLS/
# ✅ Generated .claude/settings.json
# ✅ Generated .vscode/settings.json
# ⏱️ Done in 2.8s

# 纯本地执行，无网络调用
```

### 5 个蓝图（Blueprint）

| 蓝图 | 场景 | 生成内容 |
|------|------|----------|
| `python-cli-app` | Python CLI 应用 | 项目概述、测试策略、包结构 |
| `finance-agent` | 金融场景 Agent | 安全扫描、合规检查、审计日志 |
| `rag-agent` | RAG 检索增强应用 | 知识库指南、向量数据库配置 |
| `support-agent` | 客服/支持 Agent | 知识库链接、升级流程、响应模板 |
| `workflow-agent` | 工作流自动化 Agent | 步骤定义、状态管理、错误重试 |

### 验证功能

```bash
# 运行验证合同
harnessforge verify --json

# 输出 JSON：项目Harness配置的健康检查结果
# Agent 可自动解析并自纠正
```

### MCP 服务器模式

```bash
# 启动 MCP 服务器，暴露 5 个工具
harnessforge serve

# 工具列表:
# - harness_inspect: 检查项目 Agent 配置
# - harness_verify: 运行验证合同
# - harness_blueprint: 列出/切换蓝图
# - harness_health: 项目健康度
# - harness_upgrade: 升级配置文件
```

### 金融蓝图安全扫描

```bash
# 专为 finance-agent 蓝图设计
no_trades_without_gate  # 静态安全扫描器
```

---

## 3.3.3 安装与使用

### 安装

```bash
# 需要 Python 3.10+
pip install harnessforge

# 验证
harnessforge --version
# 0.2.1
```

### 基本使用

```bash
# 快速初始化（自动检测项目类型）
cd your-project
harnessforge init

# 指定蓝图
harnessforge init --blueprint python-cli-app

# 查看当前项目配置
harnessforge inspect

# 验证配置
harnessforge verify
```

### 项目结构（初始化后）

```
your-project/
├── AGENTS.md          # Agent 行为规范
├── SOUL.md            # 项目灵魂/愿景
├── MEMORY.md          # 外部记忆指针文件
├── TOOLS.md           # 工具说明
├── SKILLS/            # 技能目录
│   ├── add-feature.md
│   └── debug.md
├── .claude/
│   └── settings.json  # Claude Code 配置
├── .vscode/
│   └── settings.json  # VS Code 配置
├── .cursor/
│   └── rules/         # Cursor 规则
└── .github/
    └── copilot-instructions.md  # Copilot 指令
```

---

## 3.3.4 核心使用场景

### 场景 1：新项目快速初始化

```bash
mkdir my-new-service && cd my-new-service
git init
harnessforge init --blueprint python-cli-app
# 3秒后 → 项目已 Agent Ready

# 然后安装 Superpowers
/plugin install superpowers@claude-plugins-official
# 项目现在既有完整配置又有流程
```

### 场景 2：存量项目改造

```bash
cd legacy-project
harnessforge init
# 自动检测项目结构，生成适配配置
# 不会覆盖已有文件（安全模式）
```

### 场景 3：多 IDE 团队

```bash
# 一次生成，所有 IDE 可用
harnessforge init
# 自动产生:
#   .claude/     → Claude Code
#   .vscode/     → VS Code + Cursor
#   .github/     → GitHub Copilot
# 团队各用各的 IDE，配置保持一致
```

---

## 3.3.5 工具间协作定位

```
HarnessForge 在 Harness 工具链中的位置：

  项目创建
     │
     ↓
  HarnessForge init  ←── 项目启动时的第一步
     │                     (3秒，一次性操作)
     ↓
  ┌──────────────────────────────┐
  │  项目已 Agent Ready          │
  │  (AGENTS.md + MEMORY.md + ···)│
  └──────────────────────────────┘
     │
     ├── → 安装 Superpowers → 获得全流程工作流
     ├── → 安装 agent-harness-kit → 获得安全护栏
     ├── → 使用 HarnessFlow → 获得多 IDE 一致体验
     └── → 直接使用 Agent → 比没有配置好很多
```

---

## 3.3.6 与 Superpowers 的集成方案

当 HarnessForge 和 Superpowers 一起使用时，推荐以下配置：

```yaml
# HarnessForge 生成的 AGENTS.md + Superpowers 技能
# 两者互补，无冲突

AGENTS.md 内容:
  - 项目身份/描述
  - 技术栈说明
  - 目录结构概览
  - 构建/测试命令
  
Superpowers 技能:
  - 提供完整的工作流方法论
  - 覆盖 AGENTS.md 不涉及的部分
  - 两者在 CLAUDE.md 中引用

最终 CLAUDE.md 结构:
  1. 项目描述（HarnessForge 生成）
  2. 技术栈（HarnessForge 生成）
  3. 技能引用（Superpowers 插件）
  4. 使用规则（手动/agent-harness-kit 补充）
```

---

## 3.3.7 已知限制与应对

| 限制 | 说明 | 对策 |
|------|------|------|
| **功能较浅** | 只生成配置骨架，没有流程 | 作为"第一步"，后续叠加其他工具 |
| **蓝图有限** | 目前只有 5 个蓝图 | 在生成的配置上手工修改 |
| **版本早期** | v0.2.1，功能还在快速迭代 | 关注更新，定期升级 |
| **纯 Python** | 需要 Python 3.10+ 环境 | CI/CD 中确保 Python 环境 |

---

## 3.3.8 安装检查清单

```
□ 已安装 harnessforge (pip install harnessforge)
□ 已在测试项目中运行 init
□ 已了解 5 个蓝图的选择标准
□ 已生成 verify --json 并理解输出
□ 已与 Superpowers 叠加测试
```

