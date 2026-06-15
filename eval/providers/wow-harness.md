# wow-harness Provider

## 概述

wow-harness 是基于 Claude Code hooks 的硬约束治理框架。与其他 Provider（CLAUDE.md 文本指令）不同，wow-harness 通过 16 个生命周期 hooks 和 16 个专业化 skills 提供**机械化的流程强制**。

## 核心差异

| | 其他 Provider | wow-harness |
|---|---|---|
| 约束方式 | 软约束（CLAUDE.md 文本） | **硬约束（hooks 拦截工具调用）** |
| 遵从率 | ~20%（Agent 自主选择） | 100%（机械执行，不可绕过） |
| 安装方式 | 复制 CLAUDE.md | `python phase2_auto.py --tier drop-in` |
| 部署复杂度 | 一个文件 | 完整脚手架（.claude/ + scripts/ + .wow-harness/） |

## 评测配置

### 安装

```bash
cd wow-harness
export WOW_HARNESS_INSTALL_HMAC_KEY=$(openssl rand -hex 32)
python scripts/install/phase2_auto.py --auto --tier drop-in --scope explicit \
  --projects "D:/Work/kleaves/Harness/eval/tmp/wow-<task-id>"
```

### 兼容性修复

评测环境（Windows + Git Bash + 批量模式）需要两处修改：

1. **python3 → python**：settings.json 中所有 hook 命令的 `python3` 替换为 `python`
2. **精简 hooks**：移除会干扰批量模式的 PostToolUse/PreToolUse/Stop hooks，仅保留 SessionStart + SessionEnd + PreCompact

### 最终 hook 配置

```
hooks:
  SessionStart:
    - session-start-reset-risk.py    # 重置风险快照
    - session-start-magic-docs.py    # 加载项目上下文
    - session-start-toolkit-reminder.py  # 展示可用 skills
  PreCompact:
    - precompact.sh                  # 压缩前保留关键上下文
  SessionEnd:
    - session-reflection.py          # 会话反思总结
    - trace-analyzer.py              # 聚合追踪数据
```

### 运行命令

```bash
cd <workspace>
claude -p "<task prompt>" --output-format json --permission-mode bypassPermissions
```

## 评分数据

12 任务总分：**1085**（排名第二，仅次于 Gstack 1090）

亮点任务：③分页排序 95、⑤Redis 缓存 90、⑩N+1 修复 80、⑪并发预订 85

## 过程评测

URL 短链接服务第四轮（交互模式），wow-harness 表现为"诊断驱动的最小修复"：
- 不重写整个项目，先分析现有代码缺陷
- 写 spec 到磁盘，用户确认后精准修复
- 只改了 3 个文件，17/17 测试通过

详见 `eval/results/process-analysis-url-shortener.md`

## 踩坑记录

1. **python3 指向 Windows Store 存根**（exit 49）→ 必须替换为 `python`
2. **stop-evaluator.py 在批量模式阻塞**（检查 progress.json，Agent 不知道填）→ 禁用了 Stop hook
3. **PostToolUse hooks 干扰编辑流程**（每次 Edit/Write 触发 3+ 个 Python 脚本）→ 仅保留 session 级 hooks
4. **INDEX.md GBK 编码**（installer 的 `read_text()` 未指定 UTF-8）→ 不影响功能，index slot fill 失败可忽略
5. **8 关状态机需要交互式 Gate 确认**→ 批量 `claude -p` 模式下不可用，Agent 在 Gate 消耗完 turns
