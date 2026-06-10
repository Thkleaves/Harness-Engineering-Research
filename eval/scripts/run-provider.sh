#!/bin/bash
# ============================================================
# run-provider.sh — Promptfoo exec provider 包装器
# 被 promptfooconfig.yaml 中每个 provider 调用
#
# 参数:
#   $1: provider label (baseline|superpowers|gstack|openspec)
#   $2: task_id (e.g. "01-validation")
#   $3: task prompt (任务描述文本)
#   $4: workspace path (初始代码)
#
# 环境变量:
#   PROVIDER_LABEL, MODEL, SKILLS
#
# 输出: Promptfoo 期望的 JSON 格式到 stdout
# ============================================================

set -euo pipefail

LABEL="${1:-baseline}"
TASK_ID="${2:-unknown}"
TASK_PROMPT="${3:-}"
WORKSPACE_SRC="${4:-}"

TMP_DIR="./tmp/${TASK_ID}-${LABEL}-$(date +%s)"
mkdir -p "$TMP_DIR"

# ── 1. 复制 workspace 到独立沙箱 ──
if [ -d "$WORKSPACE_SRC" ]; then
    cp -r "$WORKSPACE_SRC"/* "$TMP_DIR/" 2>/dev/null || true
    cp -r "$WORKSPACE_SRC"/.??* "$TMP_DIR/" 2>/dev/null || true
fi

# ── 2. 记录开始时间 ──
START_TIME=$(date +%s)

# ── 3. 调用 Claude CLI ──
# 根据 SKILLS 配置不同的系统提示词
CLAUDE_MD_PATH="$TMP_DIR/CLAUDE.md"

case "$LABEL" in
    baseline)
        # 裸 Agent — 最小 CLAUDE.md，无 skills
        cat > "$CLAUDE_MD_PATH" << 'CLAUDE_MD'
# 项目说明
这是一个 Spring Boot 项目。
请根据用户需求完成代码修改，确保代码编译通过并包含测试。
CLAUDE_MD
        ;;

    superpowers)
        # Superpowers — 注入全部 14 个技能
        cat > "$CLAUDE_MD_PATH" << 'CLAUDE_MD'
永远以中文思考和输出

# Skills
此项目使用以下 Superpowers 技能集：
- brainstorming: 实现前先澄清需求和设计方案
- writing-plans: 写实现计划
- test-driven-development: TDD 测试驱动开发
- executing-plans: 执行计划
- requesting-code-review: 请求代码审查
- receiving-code-review: 接收代码审查反馈
- verification-before-completion: 完成前验证
- finishing-a-development-branch: 完成开发分支
- systematic-debugging: 系统化调试
- subagent-driven-development: 子代理驱动开发
- dispatching-parallel-agents: 并行代理调度
- using-git-worktrees: Git worktree
- using-superpowers: 使用超能力

请严格按照 Superpowers 流程工作：brainstorming → plan → TDD → execute → review → verify → finish
CLAUDE_MD
        ;;

    gstack)
        # Gstack — 角色分工
        cat > "$CLAUDE_MD_PATH" << 'CLAUDE_MD'
# Skills
此项目使用 Gstack 角色技能集：
- gstack/ceo: 产品需求分析
- gstack/engineer: 工程实现
- gstack/qa: 质量保证和测试
- gstack/devops: 部署和运维

请在实现时依次调用各角色进行协作。
CLAUDE_MD
        ;;

    openspec)
        # OpenSpec — Spec 驱动
        cat > "$CLAUDE_MD_PATH" << 'CLAUDE_MD'
# Skills
此项目使用 OpenSpec 工作流：
- openspec: Spec 驱动的开发流程

请先写 Spec 定义所有接口和状态转换，再做实现。
CLAUDE_MD
        ;;
esac

# ── 4. 写入任务 prompt ──
TASK_FILE="$TMP_DIR/TASK.md"
echo "$TASK_PROMPT" > "$TASK_FILE"

# ── 5. 执行 Agent（在当前工作区） ──
cd "$TMP_DIR"

# 用 claude CLI 执行任务
# -p: 非交互模式，直接处理 prompt
# --output-format json: 输出 JSON 格式
AGENT_OUTPUT=$(claude -p "$(cat TASK.md)" --output-format json 2>&1) || true

# ── 6. 运行验证 ──
VERIFY_SCRIPT="../../scripts/verify.sh"
if [ -f "$VERIFY_SCRIPT" ]; then
    VERIFY_RESULT=$(bash "$VERIFY_SCRIPT" "$TMP_DIR" 2>&1) || true
else
    VERIFY_RESULT='{"pass":false,"score":0,"reason":"verify script not found"}'
fi

# ── 7. 记录结束时间和 Token ──
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# ── 8. 提取 token 消耗（从 agent 输出日志） ──
TOKEN_COUNT=0
if [ -f "$TMP_DIR/.claude/agent-logs" ]; then
    TOKEN_COUNT=$(grep -oP '"total_tokens":\s*\K\d+' "$TMP_DIR/.claude/agent-logs" 2>/dev/null | tail -1 || echo "0")
fi

# ── 9. 输出 Promptfoo 期望的 JSON ──
VERIFY_JSON=$(echo "$VERIFY_RESULT" | tail -1)
echo "{
  \"pass\": $(echo "$VERIFY_JSON" | jq -r '.pass // false'),
  \"score\": $(echo "$VERIFY_JSON" | jq -r '.score // 0'),
  \"reason\": $(echo "$VERIFY_JSON" | jq -r '.reason // "ok"'),
  \"duration\": $DURATION,
  \"tokenCount\": $TOKEN_COUNT,
  \"details\": $(echo "$VERIFY_JSON" | jq -r '.details // {}')
}"

# ── 清理（保留结果用于抽查） ──
# rm -rf "$TMP_DIR"  # 默认保留以查看产出
