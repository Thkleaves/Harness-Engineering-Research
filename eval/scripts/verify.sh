#!/bin/bash
# ============================================================
# verify.sh — Harness 评测验证脚本 v2
# 用法: ./verify.sh <workspace_dir> [task_id]
# 评分: 编译gate | 测试25 | 任务定制60 | 代码质量15 = 100
# ============================================================

WORKSPACE="${1:-.}"
TASK_ID="${2:-}"

if [ ! -d "$WORKSPACE" ]; then
    echo '{"pass":false,"score":0,"reason":"workspace not found"}'
    exit 1
fi

cd "$WORKSPACE" || exit 1

SCORE=0
COMPILE_OK=false
TEST_TOTAL=0
TEST_PASSED=0
TASK_SCORE=0
QUALITY_SCORE=0

# ═══════════════════════════════════════════════
# GATE: 编译检查
# ═══════════════════════════════════════════════
echo "[verify] mvn compile..." >&2
COMPILE_ERR=$(mktemp)
if mvn compile -q 2>"$COMPILE_ERR"; then
    COMPILE_OK=true
    rm -f "$COMPILE_ERR"
else
    COMPILE_ERR_MSG=$(cat "$COMPILE_ERR" 2>/dev/null | head -20)
    rm -f "$COMPILE_ERR"
    echo "{\"pass\":false,\"score\":0,\"reason\":\"compile failed\",\"mvn_error\":$(echo "$COMPILE_ERR_MSG" | python3 -c "import sys,json; print(json.dumps(sys.stdin.read()))" 2>/dev/null || echo '"unknown"')}"
    exit 0
fi

# ═══════════════════════════════════════════════
# 测试 (0-25 分)
# ═══════════════════════════════════════════════
echo "[verify] mvn test..." >&2
mvn test > /dev/null 2>&1 || true

TEST_LOG=$(find target/surefire-reports -name "*.txt" 2>/dev/null | head -1)
if [ -n "$TEST_LOG" ] && [ -f "$TEST_LOG" ]; then
    TEST_TOTAL=$(grep -oP 'Tests run: \K\d+' "$TEST_LOG" 2>/dev/null || echo "0")
    TEST_FAILURES=$(grep -oP 'Failures: \K\d+' "$TEST_LOG" 2>/dev/null || echo "0")
    TEST_ERRORS=$(grep -oP 'Errors: \K\d+' "$TEST_LOG" 2>/dev/null || echo "0")
    TEST_TOTAL=${TEST_TOTAL:-0}
    TEST_FAILURES=${TEST_FAILURES:-0}
    TEST_ERRORS=${TEST_ERRORS:-0}
    TEST_PASSED=$((TEST_TOTAL - TEST_FAILURES - TEST_ERRORS))

    if [ "$TEST_TOTAL" -gt 0 ] 2>/dev/null; then
        # 有测试文件 + 有通过：基础 10 分
        SCORE=$((SCORE + 10))
        # 通过率分：最多 15 分，按比例
        PASS_RATE_BONUS=$(awk "BEGIN {printf \"%.0f\", ($TEST_PASSED/$TEST_TOTAL)*15}" 2>/dev/null || echo "0")
        SCORE=$((SCORE + PASS_RATE_BONUS))
    fi
else
    # 无测试报告 — 检查测试文件
    TEST_FILES=$(find src/test -name "*.java" 2>/dev/null | wc -l 2>/dev/null || echo "0")
    if [ "${TEST_FILES:-0}" -gt 0 ] 2>/dev/null; then
        # 有测试文件但无法运行 — 编译错误
        SCORE=$((SCORE + 2))  # 有尝试
    fi
fi

# ═══════════════════════════════════════════════
# 任务定制断言 (0-60 分)
# ═══════════════════════════════════════════════
echo "[verify] task checks for '$TASK_ID'..." >&2

check_file_contains() {  # pattern, glob, points, label
    local pat="$1" glob="$2" pts="$3" label="$4"
    if find src/main -name "$glob" -exec grep -l "$pat" {} \; 2>/dev/null | grep -q .; then
        TASK_SCORE=$((TASK_SCORE + pts))
        echo "  +${pts} ${label}" >&2
    else
        echo "    0 ${label} (not found)" >&2
    fi
}

check_any_file_contains() {  # pattern, points, label
    local pat="$1" pts="$2" label="$3"
    if grep -rl "$pat" src --include="*.java" 2>/dev/null | grep -q .; then
        TASK_SCORE=$((TASK_SCORE + pts))
        echo "  +${pts} ${label}" >&2
    else
        echo "    0 ${label} (not found)" >&2
    fi
}

check_test_contains() {
    local pat="$1" pts="$2" label="$3"
    if grep -rl "$pat" src/test --include="*.java" 2>/dev/null | grep -q .; then
        TASK_SCORE=$((TASK_SCORE + pts))
        echo "  +${pts} ${label}" >&2
    else
        echo "    0 ${label} (not found)" >&2
    fi
}

check_xml_contains() {
    local pat="$1" pts="$2" label="$3"
    if grep -q "$pat" pom.xml 2>/dev/null; then
        TASK_SCORE=$((TASK_SCORE + pts))
        echo "  +${pts} ${label}" >&2
    else
        echo "    0 ${label} (not found)" >&2
    fi
}

case "$TASK_ID" in
    01-validation)
        # @Valid 在 Controller (10)
        check_any_file_contains "@Valid" 10 "@Valid on Controller param"
        # DTO 有校验注解 (15)
        check_any_file_contains "@NotBlank" 5 "@NotBlank on DTO"
        check_any_file_contains "@Email" 5 "@Email on DTO"
        check_any_file_contains "@Size\|@Min\|@Max\|@Pattern" 5 "size/range/pattern annotations"
        # GlobalExceptionHandler (10)
        check_any_file_contains "ExceptionHandler\|ControllerAdvice\|RestControllerAdvice" 10 "global exception handler"
        # 测试存在 (15)
        check_test_contains "@Test" 5 "has test class"
        check_test_contains "MockMvc\|TestRestTemplate\|WebTestClient" 5 "integration test style"
        # pom 有 validation (10)
        check_xml_contains "spring-boot-starter-validation" 10 "validation dependency"
        ;;

    02-actuator)
        check_xml_contains "spring-boot-starter-actuator" 15 "actuator dependency"
        check_any_file_contains "HealthIndicator" 15 "custom HealthIndicator"
        check_any_file_contains "ReadinessIndicator\|readiness\|Readiness" 10 "readiness endpoint"
        check_any_file_contains "@SpringBootTest\|TestRestTemplate" 10 "integration test"
        check_any_file_contains "actuator/health" 10 "health endpoint in test"
        ;;

    03-pagination)
        check_any_file_contains "page\|Page\b" 10 "pagination parameter"
        check_any_file_contains "sort\|Sort\|SORT" 5 "sort parameter"
        check_any_file_contains "ALLOWED\|whitelist\|白名单\|allowedSort\|SORTABLE" 15 "sort whitelist"
        check_any_file_contains "UserSummary\|Summary" 10 "UserSummary DTO"
        # UserSummary 不含 password 字段 (15): 找到 Summary 文件，检查不含 password
        if find src/main -name "*Summary*" -o -name "*DTO*" 2>/dev/null | xargs grep -l "password" 2>/dev/null | grep -q .; then
            echo "    0 UserSummary leaks password" >&2
        else
            TASK_SCORE=$((TASK_SCORE + 15))
            echo "  +15 UserSummary no password leak" >&2
        fi
        check_any_file_contains "totalPages\|totalElements\|total" 5 "pagination metadata"
        ;;

    04-register)
        check_any_file_contains "register" 10 "register endpoint"
        check_any_file_contains "verify.*token\|token.*verify\|/verify" 10 "verify endpoint"
        check_any_file_contains "login\|/auth/login" 10 "login endpoint"
        check_any_file_contains "UNVERIFIED" 10 "UNVERIFIED status"
        check_any_file_contains "expir\|过期\|24.*hour\|plus.*hour\|plusDay\|plus.*Day" 10 "token expiry"
        check_test_contains "register\|verify\|login" 10 "auth flow test"
        ;;

    05-redis-cache)
        check_xml_contains "spring-boot-starter-cache" 10 "cache starter"
        check_xml_contains "spring-boot-starter-data-redis" 10 "redis starter"
        check_any_file_contains "@Cacheable" 10 "@Cacheable"
        check_any_file_contains "@CacheEvict\|@CachePut" 10 "@CacheEvict or @CachePut"
        check_any_file_contains "time-to-live\|ttl\|TTL\|expir" 10 "TTL configuration"
        check_any_file_contains "@EnableCaching" 10 "@EnableCaching"
        ;;

    06-ratelimit)
        check_any_file_contains "bucket\|Bucket\|TokenBucket\|RateLimit" 15 "TokenBucket implementation"
        check_any_file_contains "429\|TOO_MANY_REQUESTS\|Too Many" 10 "HTTP 429 response"
        check_any_file_contains "Retry-After\|RetryAfter" 10 "Retry-After header"
        check_any_file_contains "ConcurrentHashMap" 10 "thread-safe (ConcurrentHashMap)"
        check_test_contains "thread\|concurrent\|CountDownLatch\|ExecutorService" 15 "concurrent test"
        ;;

    07-jwt-auth)
        check_any_file_contains "Jwt\|JWT\|jjwt\|TokenProvider" 10 "JWT implementation"
        check_any_file_contains "refresh.*token\|/auth/refresh" 10 "refresh token"
        check_any_file_contains "blacklist\|logout\|失效\|revoke" 10 "token blacklist"
        check_any_file_contains "filter\|Interceptor\|OncePerRequest" 10 "auth filter/interceptor"
        check_any_file_contains "ROLE_\|hasRole\|hasAuthority\|ADMIN" 10 "role-based access"
        check_test_contains "Jwt\|token\|auth" 10 "auth test coverage"
        ;;

    08-refactor-layers)
        check_any_file_contains "@Repository" 10 "Repository layer"
        check_any_file_contains "@Service" 10 "Service layer"
        # Controller 不含数据存储逻辑 (ConcurrentHashMap 不在 controller 包)
        if find src/main -path "*/controller/*" -exec grep -l "ConcurrentHashMap\|AtomicLong" {} \; 2>/dev/null | grep -q .; then
            echo "    0 Controller still has data access" >&2
        else
            TASK_SCORE=$((TASK_SCORE + 15))
            echo "  +15 Controller clean (no data access)" >&2
        fi
        check_test_contains "ServiceTest\|RepositoryTest" 15 "Service/Repository tests"
        # 原有测试仍通过
        if [ "$TEST_FAILURES" -eq 0 ] 2>/dev/null && [ "$TEST_ERRORS" -eq 0 ] 2>/dev/null && [ "$TEST_TOTAL" -gt 0 ] 2>/dev/null; then
            TASK_SCORE=$((TASK_SCORE + 10))
            echo "  +10 regression tests pass" >&2
        fi
        ;;

    09-file-upload)
        check_any_file_contains "upload\|MultipartFile" 10 "file upload endpoint"
        check_any_file_contains "@Async\|TaskExecutor\|ThreadPool" 10 "async processing"
        check_any_file_contains "status\|PENDING\|PROCESSING\|DONE" 10 "status tracking"
        check_any_file_contains "max.*size\|MaxUploadSize\|max-file-size" 10 "file size limit"
        check_any_file_contains "retry\|Retryable\|重试" 10 "retry mechanism"
        check_test_contains "upload\|file\|csv" 10 "upload test"
        ;;

    10-nplusone-fix)
        check_any_file_contains "JOIN FETCH\|@EntityGraph\|fetch.*join" 20 "JOIN FETCH or EntityGraph"
        check_any_file_contains "count.*SQL\|StatementInspector\|datasource-proxy\|query.*count" 15 "SQL count verification"
        # 测试存在
        check_test_contains "NPlusOne\|n.plus.1\|sql.*count\|query.*count" 15 "N+1 test"
        # API 兼容（不包含新字段）
        if grep -rl "userName\|getUserName\|user_name" src/main --include="*Response*" 2>/dev/null | grep -q .; then
            TASK_SCORE=$((TASK_SCORE + 10))
            echo "  +10 API response preserved" >&2
        fi
        ;;

    11-concurrent-booking)
        check_any_file_contains "@Version" 15 "@Version optimistic lock"
        check_any_file_contains "OptimisticLockException\|StaleStateException" 10 "lock exception handling"
        check_any_file_contains "@Retryable\|retry\|重试" 10 "retry mechanism"
        check_any_file_contains "@Index\|create index\|CREATE INDEX" 5 "database index"
        check_test_contains "thread\|concurrent\|CountDownLatch\|ExecutorService\|100" 15 "concurrent test (100 threads)"
        # 验证无超卖：测试中检查了库存
        check_test_contains "assert.*quantity\|assert.*stock\|assert.*remaining\|assertEquals.*[0-9]" 5 "quantity assertion"
        ;;

    12-flaky-test)
        check_any_file_contains "Clock\b" 15 "Clock injection"
        check_any_file_contains "fixed\|Clock.fixed\|Instant.now" 10 "Clock.fixed() in test"
        check_any_file_contains "ZoneId\|ZoneOffset\|UTC\|Asia/Shanghai\|America/New_York" 10 "timezone handling"
        check_test_contains "UTC\|Asia/Shanghai\|America/New_York" 10 "multi-timezone test"
        # 根因解释
        if grep -rl "LocalDate.now\|时区\|timezone\|zone" src --include="*.java" 2>/dev/null | grep -v "test" | grep -v "import" | grep -q .; then
            echo "    0 still uses LocalDate.now() without Clock" >&2
        else
            TASK_SCORE=$((TASK_SCORE + 15))
            echo "  +15 LocalDate.now() removed/fixed" >&2
        fi
        ;;

    *)
        # 无任务ID — 用通用检查
        echo "  (no task-specific checks)" >&2
        ;;
esac

SCORE=$((SCORE + TASK_SCORE))

# ═══════════════════════════════════════════════
# 代码质量 (0-15 分)
# ═══════════════════════════════════════════════
echo "[verify] code quality..." >&2

# 异常处理
if grep -rn "ExceptionHandler\|ControllerAdvice\|try.*catch" src/main --include="*.java" 2>/dev/null | grep -q .; then
    QUALITY_SCORE=$((QUALITY_SCORE + 5))
    echo "  +5 exception handling" >&2
fi

# 校验使用
if grep -rn "@Valid\|@Validated\|@NotBlank\|@NotNull\|@Positive" src/main --include="*.java" 2>/dev/null | grep -q .; then
    QUALITY_SCORE=$((QUALITY_SCORE + 5))
    echo "  +5 input validation" >&2
fi

# 分层清晰度
CONTROLLER_COUNT=$(find src/main -path "*/controller/*" -name "*.java" 2>/dev/null | wc -l)
SERVICE_COUNT=$(find src/main -path "*/service/*" -name "*.java" 2>/dev/null | wc -l)
if [ "$CONTROLLER_COUNT" -gt 0 ] && [ "$SERVICE_COUNT" -gt 0 ]; then
    QUALITY_SCORE=$((QUALITY_SCORE + 5))
    echo "  +5 layered architecture" >&2
fi

SCORE=$((SCORE + QUALITY_SCORE))

# ═══════════════════════════════════════════════
# 输出
# ═══════════════════════════════════════════════
cat << EOF
{
  "pass": true,
  "score": $SCORE,
  "maxScore": 100,
  "compile": "true",
  "tests": {
    "total": $TEST_TOTAL,
    "passed": $TEST_PASSED,
    "failures": $TEST_FAILURES,
    "errors": $TEST_ERRORS
  },
  "taskScore": $TASK_SCORE,
  "qualityScore": $QUALITY_SCORE
}
EOF
