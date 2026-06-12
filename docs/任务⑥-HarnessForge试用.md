# 任务⑥ — HarnessForge 试用总结

> 日期: 2026-06-09 | 工作空间: 04-book-api-forge | 版本: v0.2.2
> 蓝图: workflow-agent v1.0.0

## 执行流程

```bash
cd Harness/workspaces/04-book-api-forge
harnessforge init --blueprint workflow-agent   # 生成全套配置
harnessforge inspect                            # 检测项目特征
harnessforge verify --json                       # 验证配置健康度
```

## 生成内容（~20 个文件）

```
04-book-api-forge/
├── AGENTS.md              # 通用 Agent 行为规范
├── SOUL.md                # 项目描述/愿景
├── MEMORY.md              # 跨会话记忆指针
├── TOOLS.md               # 可用工具说明
├── SKILLS/                # 3 个内置技能
│   ├── call-tool-with-retry/
│   ├── check-result/
│   └── decompose-task/
├── .claude/CLAUDE.md      # Claude Code 专用指令
├── .cursor/rules/         # Cursor 规则
├── .continue/config.json  # Continue 配置
├── .windsurf/rules/       # Windsurf 规则
├── .harness/
│   ├── profile.yaml       # 项目 profile
│   ├── manifest.json       # 文件清单
│   └── memory_schemas/     # 记忆数据结构
├── scripts/
│   ├── test_task.sh
│   └── verify_output.py
└── harness.config.json
```

## inspect 检测结果

| 字段 | 检出值 | 实际情况 |
|------|--------|----------|
| languages | java ✅ | 正确 |
| frameworks | 无 ❌ | 实际是 Spring Boot |
| package mgrs | 无 ❌ | 实际是 Maven (pom.xml) |
| tests | 无 | — |
| build | 无 ❌ | 实际有 Maven |
| git | 无 | 本地 git，无远程 |
| 已有配置 | 5 个 agent config 检测到 ✅ | CLAUDE.md/AGENTS.md/Cursor/Continue/Windsurf |
| 文件数 | 31 | — |
| 代码行 | ~2007 | — |

**结论: inspect 的语言检测准确，但框架/构建工具识别能力较弱。**

## verify 校验结果

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| structure | ✅ pass | 所有生成文件存在且可解析 |
| tool_log | ⏭️ skipped | 无 agent 执行记录（未跑过任务） |
| idempotent | ⏭️ skipped | 同上 |

## 与 agent-harness-kit 的对比

| 维度 | HarnessForge (v0.2.2) | agent-harness-kit (v0.22.3) |
|------|----------------------|---------------------------|
| **定位** | "3 秒让项目 Agent Ready" | "生产级 Agent 基础设施" |
| **安装方式** | `pip install harnessforge` | `npm install -g agent-harness-kit` |
| **生成文件数** | ~20 | ~200+ |
| **初始化速度** | <3 秒 | ~10 秒（含编译） |
| **学习成本** | ~0.5 天 | ~5-7 天 |
| **内置技能** | 3 个 | 33 个 |
| **评审 Agent** | 无 | 10 个 reviewer agent |
| **Hook 系统** | 无 | 9 组 Hook 事件 |
| **可观测性** | 无 | SQLite 状态追踪 + 成本管控 |
| **Failures→Rules** | 无 | ✅ |
| **跨 IDE** | ✅ Claude/Cursor/Continue/Windsurf | ⚠️ 主要 Claude Code |
| **蓝图/预设** | 5 个蓝图 | 3 个 policy pack |
| **框架检测** | ⚠️ 弱（漏检 Spring Boot） | ✅ 自动识别 |
| **成熟度** | 早期（v0.2.2, ~200 stars） | 较成熟（v0.22.3, ~500 stars） |

## 关键发现

1. **HarnessForge 解决的是"从 0 到 1"的问题** — 让项目目录变成 Agent 可工作的环境，只需要一条命令。kit 解决的是"从 1 到 100"的问题 — 安全、治理、可观测性。

2. **适用场景完全不同**：
   - 新项目第一天 → HarnessForge
   - 已有项目需要深度治理 → agent-harness-kit
   - 两者可以叠加：先 Forge 搭骨架，再 kit 加护栏

3. **框架检测是短板** — Spring Boot + Maven 的项目，inspect 全部漏检。在实际使用中需要手动补 profile.yaml。

4. **跨 IDE 支持是亮点** — 一次 init，Claude Code、Cursor、Continue、Windsurf 全部可用，团队中各用各的 IDE 不成问题。

5. **verify 机制有潜力但尚浅** — 3 项检查只有一个有关联（structure），另外两个依赖 agent 实际执行记录。对比 kit 的 23 个 readiness gate，差距明显。
