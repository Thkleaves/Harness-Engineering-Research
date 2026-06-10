#!/bin/bash
# ============================================================
# run-eval.sh — 评测运行入口
# 用法:
#   ./run-eval.sh pilot             ← Pilot: 只跑任务③，4组×1次
#   ./run-eval.sh l1                ← L1 批量: 任务①②, 4组×2次
#   ./run-eval.sh l2                ← L2 批量: 任务③④⑤⑥, 4组×2次
#   ./run-eval.sh all               ← 全部 Day2 任务
#   ./run-eval.sh manual <task-id>  ← 手动跑单个任务
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
EVAL_DIR="$(dirname "$SCRIPT_DIR")"
WORKSPACES_DIR="$EVAL_DIR/workspaces"
RESULTS_DIR="$EVAL_DIR/results"
TASKS_DIR="$EVAL_DIR/tasks"
TMP_DIR="$EVAL_DIR/tmp"

mkdir -p "$RESULTS_DIR" "$TMP_DIR"

MODE="${1:-help}"

# Provider 配置
PROVIDERS=("baseline" "superpowers" "gstack" "openspec")

# Task ID → workspace 映射
declare -A TASK_WS
TASK_WS["01-validation"]="01-validation"
TASK_WS["02-actuator"]="02-actuator"
TASK_WS["03-pagination"]="03-pagination"
TASK_WS["04-register"]="04-register"
TASK_WS["05-redis-cache"]="05-redis-cache"
TASK_WS["06-ratelimit"]="06-ratelimit"
TASK_WS["07-jwt-auth"]="07-jwt-auth"
TASK_WS["08-refactor-layers"]="08-refactor-layers"
TASK_WS["09-file-upload"]="09-file-upload"
TASK_WS["10-nplusone-fix"]="10-nplusone-fix"
TASK_WS["11-concurrent-booking"]="11-concurrent-booking"
TASK_WS["12-flaky-test"]="12-flaky-test"

# ── 公共指令：自行决策不阻塞 ──
# 关键：不禁 brainstorming/plan，但禁止 AskUserQuestion（无人应答会阻塞）
# 所有设计决策自己分析权衡后直接做选择，不要等待用户输入
NO_ASK="遇到设计决策请自行分析各选项的利弊，做出选择后直接实现。不要使用 AskUserQuestion 工具——无人会回答，会阻塞流程。不要等待用户确认，做完全部代码实现和测试。"

# Task prompt 映射
declare -A TASK_PROMPT
TASK_PROMPT["01-validation"]='现有 UserCreateRequest DTO 没有任何校验注解。
请为以下字段添加 Jakarta Validation 注解：
- username: 非空, 3-50 字符, 字母数字
- email: 合法邮箱格式
- password: 最小 8 位, 含大小写+数字
- age: 18-120
同时：Controller 参数加 @Valid，添加全局异常处理返回 400 + 错误信息，写测试验证合法/非法输入。'

TASK_PROMPT["02-actuator"]='请为当前 Spring Boot 应用添加 Actuator 支持：
- 添加 spring-boot-starter-actuator 依赖
- 暴露 /actuator/health 端点（含数据库连接检查）
- 暴露 /actuator/info（含应用版本和构建时间）
- 自定义 /actuator/health/readiness 检查 Redis 连接
- 添加集成测试验证端点响应'

TASK_PROMPT["03-pagination"]='现有 GET /api/users 返回所有用户，无分页。
请实现：
1. 添加分页参数 page, size（默认 page=0, size=20, 最大 size=100）
2. 添加排序参数 sort（支持 username, email, age，默认 id,asc）
3. 返回 Page<UserSummary>（不暴露 password 字段）
4. 排序白名单过滤（不允许按 password 排序 — 防 SQL 注入）
5. Controller、Service、Repository 三层都要有测试
6. 响应包含 totalPages, totalElements 等分页元数据'

TASK_PROMPT["04-register"]='现有 User 实体和基本 CRUD，没有注册流程。
请实现完整的用户注册+邮箱验证：
1. POST /api/auth/register: 创建用户，状态为 UNVERIFIED
2. 生成验证 Token（UUID），保存到用户记录，"发送验证邮件"存 log
3. GET /api/auth/verify?token=xxx: 验证邮箱，激活用户（状态改为 ACTIVE）
4. 未验证用户不能登录（POST /api/auth/login 拒绝 UNVERIFIED 用户）
5. Token 有过期机制（24 小时）
6. 测试覆盖：正常注册→验证→登录，Token 过期，重复验证，未验证登录被拒'

TASK_PROMPT["05-redis-cache"]='现有 GET /api/products/{id} 每次都查内存 Map，响应慢。
请添加 Redis 缓存层：
1. 添加 spring-boot-starter-cache + spring-boot-starter-data-redis 依赖
2. @Cacheable 在 Service 层的 findById 方法
3. 更新产品时 @CacheEvict（或 @CachePut）
4. 删除产品时 @CacheEvict
5. 缓存 TTL 可配置（application.yml）
6. 测试验证：缓存命中/未命中/更新后失效
注意：测试不需要真实 Redis，用 @SpringBootTest + mock 或 @TestConfiguration 即可'

TASK_PROMPT["06-ratelimit"]='当前 GET /api/users 是公共 API，被过度调用。
请实现 Token Bucket 限流算法：
1. 实现 Token Bucket 算法（不用第三方库，手写算法）
2. 每个用户独立桶（按 X-User-Id 请求头区分），每分钟最多 60 个请求
3. 超过限额返回 429 Too Many Requests + Retry-After 响应头
4. 桶参数可配置（容量、填充速率）在 application.yml
5. 线程安全（ConcurrentHashMap）
6. 写并发测试验证限流正常工作'

TASK_PROMPT["07-jwt-auth"]='当前是一个全新的 Spring Boot 项目骨架，没有任何认证机制。
请从零实现完整的 JWT 认证：
1. POST /api/auth/register — 用户注册
2. POST /api/auth/login — 返回 Access Token + Refresh Token
3. JWT 拦截器 — 验证所有 /api/** 请求的 Token（排除 /api/auth/**）
4. POST /api/auth/refresh — 用 Refresh Token 换新 Access Token
5. POST /api/auth/logout — Token 加入黑名单，登出后失效
6. 基于角色权限：ADMIN 可访问 /api/admin/**，USER 只能访问 /api/users/**
7. 编写完整测试覆盖：注册→登录→访问受保护资源→Refresh→登出→Token 失效'

TASK_PROMPT["08-refactor-layers"]='当前 UserController 中有大量业务逻辑混在控制器里。
请进行三层架构重构：
1. 将数据存取逻辑抽取到 UserRepository（@Repository, ConcurrentHashMap 存储）
2. 将业务逻辑抽取到 UserService（@Service, 包含校验、业务规则）
3. Controller 只做参数校验和路由（用 @Valid + @Validated）
4. 保留所有现有 API 行为不变
5. 不能破坏现有的集成测试
6. 重构后新增 Service 层单元测试和 Repository 层单元测试'

TASK_PROMPT["09-file-upload"]='请实现文件上传 + 异步处理功能：
1. POST /api/files/upload — 接收 CSV 文件上传，返回 fileId
2. 后台异步处理（@Async + TaskExecutor）：解析 CSV，数据批量入库
3. GET /api/files/{id}/status — 查询处理状态
4. 文件大小限制 10MB（配置在 application.yml）
5. 处理失败有重试机制（最多 3 次，指数退避）
6. CSV 解析失败的坏行记录到 errors_{fileId}.csv
7. 编写测试：上传→状态查询→处理完成、超大文件拒绝、坏行处理'

TASK_PROMPT["10-nplusone-fix"]='当前 GET /api/orders 接口存在 N+1 查询问题，Order 有 @ManyToOne User，每次查订单后单独查用户。
请修复：
1. 使用 @EntityGraph 或 JOIN FETCH 将查询优化为 1 次 SQL
2. 添加测试验证 SQL 查询次数从 N+1 降为 1
3. 验证响应时间改善
4. 不改变 API 返回的 JSON 结构
5. 编写回归测试确保修复不影响现有功能'

TASK_PROMPT["11-concurrent-booking"]='当前票务预订系统没有并发保护。
请实现并发安全的预订：
1. 在 Ticket 实体添加 @Version 乐观锁
2. POST /api/tickets/{id}/book?quantity=N — 捕获 OptimisticLockException
3. 乐观锁冲突时自动重试（最多 3 次）
4. 返回准确的剩余库存
5. 编写并发测试：100 线程并发预订，验证无超卖'

TASK_PROMPT["12-flaky-test"]='当前有一个时区相关的 Flaky 测试：SalesReportService 用 LocalDate.now() 过滤今天的订单，CI(UTC) 和本地(Asia/Shanghai) 的"今天"定义不一致导致跨日期边界时测试随机失败。
请修复：
1. 定位根因
2. Clock 注入替代 LocalDate.now()
3. 测试用 Clock.fixed() 固定时间
4. 参数化测试覆盖 UTC, Asia/Shanghai, America/New_York 三个时区
5. 解释根因和修复方案'

# ═════════════════════════════════════════════════╗
# 函数                                              ║
# ═════════════════════════════════════════════════╝

run_single() {
    local task_id="$1"
    local provider="$2"
    local run_num="${3:-1}"
    local workspace="$WORKSPACES_DIR/${TASK_WS[$task_id]}"
    local sandbox="$TMP_DIR/${task_id}-${provider}-run${run_num}"

    echo "  [$provider] 准备沙箱..."
    rm -rf "$sandbox"
    cp -r "$workspace" "$sandbox"

    # 写 CLAUDE.md — 统一从 providers/ 目录复制
    local provider_md="$EVAL_DIR/providers/$provider.md"
    if [ -f "$provider_md" ]; then
        cp "$provider_md" "$sandbox/CLAUDE.md"
    else
        echo "# Spring Boot project. Complete all code and tests without asking questions." > "$sandbox/CLAUDE.md"
    fi

    echo "  [$provider] 执行中..."
    local start_time=$(date +%s)

    cd "$sandbox"
    # --permission-mode bypassPermissions: 自动批准文件操作
    # 不加 --max-turns：Agent 做完自动 end_turn，设上限会人为截断长流程组
    claude -p "${TASK_PROMPT[$task_id]}. ${NO_ASK}" \
           --output-format json \
           --permission-mode bypassPermissions \
           2>&1 | tee "$RESULTS_DIR/${task_id}-${provider}-run${run_num}.log" || true

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    echo "  [$provider] 验证中..."
    cd "$EVAL_DIR"
    # 用临时文件避免管道捕获问题；传 task_id 启用任务定制断言
    local verify_json="$RESULTS_DIR/.verify-${task_id}-${provider}-run${run_num}.json"
    bash "$SCRIPT_DIR/verify.sh" "$sandbox" "$task_id" > "$verify_json" 2>/dev/null
    # 从 JSON 文件直接读 score
    local score=0
    if [ -f "$verify_json" ] && [ -s "$verify_json" ]; then
        score=$(grep -o '"score" *: *[0-9]*' "$verify_json" | head -1 | grep -o '[0-9]*$')
    fi
    score=${score:-0}

    # 从日志提取 token 消耗
    local token_in=0 token_out=0 token_cache=0 stop_reason="unknown"
    local log_file="$RESULTS_DIR/${task_id}-${provider}-run${run_num}.log"
    if [ -f "$log_file" ]; then
        token_in=$(grep -oP '"input_tokens":\s*\K\d+' "$log_file" 2>/dev/null | head -1)
        token_out=$(grep -oP '"output_tokens":\s*\K\d+' "$log_file" 2>/dev/null | head -1)
        stop_reason=$(grep -oP '"stop_reason":\s*"\K[^"]+' "$log_file" 2>/dev/null | head -1)
        token_in=${token_in:-0}; token_out=${token_out:-0}
        local cache1=$(grep -oP '"cacheReadInputTokens":\s*\K\d+' "$log_file" 2>/dev/null | head -1)
        local cache2=$(grep -oP '"cacheReadInputTokens":\s*\K\d+' "$log_file" 2>/dev/null | tail -1)
        token_cache=$(( ${cache1:-0} + ${cache2:-0} ))
    fi

    echo "  [$provider] ✓ 得分:${score} 耗时:${duration}s io:$((token_in+token_out)) cache:${token_cache} stop:${stop_reason}"

    # 追加到 CSV
    local csv_file="$RESULTS_DIR/eval-results.csv"
    if [ ! -f "$csv_file" ]; then
        echo "task_id,provider,run,score,duration_sec,token_in,token_out,token_cache,stop_reason" > "$csv_file"
    fi
    echo "$task_id,$provider,$run_num,$score,$duration,$token_in,$token_out,$token_cache,$stop_reason" >> "$csv_file"
}

# ═════════════════════════════════════════════════╗
# 模式分发                                          ║
# ═════════════════════════════════════════════════╝

case "$MODE" in
    pilot)
        echo "=== Pilot 试跑: 任务③ 分页排序 ==="
        echo "4 Provider × 1 次 = 4 条"
        for provider in "${PROVIDERS[@]}"; do
            run_single "03-pagination" "$provider" 1
        done
        echo "=== Pilot 完成 ==="
        ;;

    l1)
        echo "=== L1 批量: 任务①② ==="
        echo "4 Provider × 2 任务 × 1 次 = 8 条"
        for run in 1; do
            for task in "01-validation" "02-actuator"; do
                for provider in "${PROVIDERS[@]}"; do
                    run_single "$task" "$provider" "$run"
                done
            done
        done
        echo "=== L1 完成 ==="
        ;;

    l2)
        echo "=== L2 批量: 任务③④⑤⑥ ==="
        echo "4 Provider × 4 任务 × 1 次 = 16 条"
        for run in 1; do
            for task in "03-pagination" "04-register" "05-redis-cache" "06-ratelimit"; do
                for provider in "${PROVIDERS[@]}"; do
                    run_single "$task" "$provider" "$run"
                done
            done
        done
        echo "=== L2 完成 ==="
        ;;

    l3)
        echo "=== L3 批量: 任务⑦⑧⑨⑩ ==="
        echo "4 Provider × 4 任务 × 3 次 = 48 条"
        for run in 1 2 3; do
            for task in "07-jwt-auth" "08-refactor-layers" "09-file-upload" "10-nplusone-fix"; do
                for provider in "${PROVIDERS[@]}"; do
                    run_single "$task" "$provider" "$run"
                done
            done
        done
        echo "=== L3 完成 ==="
        ;;

    l4)
        echo "=== L4 批量: 任务⑪⑫ ==="
        echo "4 Provider × 2 任务 × 3 次 = 24 条"
        for run in 1 2 3; do
            for task in "11-concurrent-booking" "12-flaky-test"; do
                for provider in "${PROVIDERS[@]}"; do
                    run_single "$task" "$provider" "$run"
                done
            done
        done
        echo "=== L4 完成 ==="
        ;;

    all)
        echo "=== Day 2-3 全量: L1(8) + L2(16) + L3(48) + L4(24) ==="
        echo "方案B: 预计 96 条记录"
        bash "$0" l1
        bash "$0" l2
        bash "$0" l3
        bash "$0" l4
        ;;

    manual)
        if [ -z "${2:-}" ]; then
            echo "用法: $0 manual <task-id> [provider]"
            echo "  task-id: 01-validation, 02-actuator, 03-pagination, 04-register, 05-redis-cache, 06-ratelimit"
            exit 1
        fi
        TASK="$2"
        PROVIDER="${3:-superpowers}"
        run_single "$TASK" "$PROVIDER" 1
        ;;

    help|*)
        echo "用法: $0 <mode>"
        echo ""
        echo "  pilot              Pilot 试跑（任务③, 4组×1次=4条）"
        echo "  l1                 L1 批量（任务①②, 4组×1次=8条）"
        echo "  l2                 L2 批量（任务③④⑤⑥, 4组×1次=16条）"
        echo "  l3                 L3 批量（任务⑦⑧⑨⑩, 4组×3次=48条）"
        echo "  l4                 L4 批量（任务⑪⑫, 4组×3次=24条）"
        echo "  all                全量（96条）"
        echo "  manual <task-id>   手动跑单个任务"
        echo ""
        echo "任务ID: 01-validation  02-actuator  03-pagination"
        echo "       04-register    05-redis-cache  06-ratelimit"
        echo "       07-jwt-auth    08-refactor-layers  09-file-upload"
        echo "       10-nplusone-fix  11-concurrent-booking  12-flaky-test"
        ;;
esac
