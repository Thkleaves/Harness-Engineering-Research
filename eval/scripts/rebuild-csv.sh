#!/bin/bash
# ============================================================
# rebuild-csv.sh — 从所有 log + verify 重建 eval-results.csv
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
EVAL_DIR="$(dirname "$SCRIPT_DIR")"
RESULTS_DIR="$EVAL_DIR/results"
TMP_DIR="$EVAL_DIR/tmp"

CSV="$RESULTS_DIR/eval-results.csv"
echo "task_id,provider,run,score,duration_sec,token_in,token_out,token_cache,stop_reason" > "$CSV"

echo "=== 重新验证所有沙箱 ==="
for sandbox in "$TMP_DIR"/*-run*; do
    [ -d "$sandbox" ] || continue
    dirname=$(basename "$sandbox")
    # 解析: task_id-provider-runN
    # task_id 可能含连字符 (如 01-validation)，provider 可能是 baseline|superpowers|gstack|openspec
    # 从末尾反向解析: 去掉 -runN 后缀，再找最后的 provider

    # 提取 run 编号
    run=$(echo "$dirname" | grep -oP 'run\d+$')

    # 去掉 -runN 后缀
    base="${dirname%-run*}"

    # 从末尾匹配 provider
    for prov in openspec superpowers gstack baseline; do
        if [[ "$base" == *"-$prov" ]]; then
            provider="$prov"
            task_id="${base%-$prov}"
            break
        fi
    done

    if [ -z "${task_id:-}" ] || [ -z "${provider:-}" ]; then
        echo "  SKIP: 无法解析 $dirname"
        continue
    fi

    # 重新跑 verify
    verify_json="$RESULTS_DIR/.verify-${task_id}-${provider}-${run}.json"
    echo "  [$task_id] [$provider] $run — 验证中..."
    bash "$SCRIPT_DIR/verify.sh" "$sandbox" "$task_id" > "$verify_json" 2>/dev/null || true

    score=0
    if [ -f "$verify_json" ] && [ -s "$verify_json" ]; then
        score=$(grep -o '"score" *: *[0-9]*' "$verify_json" | head -1 | grep -o '[0-9]*$')
    fi
    score=${score:-0}
    echo "    score=$score"

    # 从 log 提取数据
    log_file="$RESULTS_DIR/${task_id}-${provider}-${run}.log"
    token_in=0; token_out=0; token_cache=0; duration=0; stop_reason="unknown"

    if [ -f "$log_file" ]; then
        token_in=$(grep -oP '"input_tokens":\s*\K\d+' "$log_file" 2>/dev/null | head -1)
        token_out=$(grep -oP '"output_tokens":\s*\K\d+' "$log_file" 2>/dev/null | head -1)
        stop_reason=$(grep -oP '"stop_reason":\s*"\K[^"]+' "$log_file" 2>/dev/null | head -1)
        duration_ms=$(grep -oP '"duration_ms":\s*\K\d+' "$log_file" 2>/dev/null | head -1)
        cache1=$(grep -oP '"cache_read_input_tokens":\s*\K\d+' "$log_file" 2>/dev/null | head -1)
        cache2=$(grep -oP '"cache_read_input_tokens":\s*\K\d+' "$log_file" 2>/dev/null | tail -1)
        token_in=${token_in:-0}; token_out=${token_out:-0}
        token_cache=$(( ${cache1:-0} + ${cache2:-0} ))
        duration=$(( ${duration_ms:-0} / 1000 ))
    fi

    stop_reason=${stop_reason:-unknown}

    echo "    tokens: in=$token_in out=$token_out cache=$token_cache dur=${duration}s stop=$stop_reason"
    echo "$task_id,$provider,$run,$score,$duration,$token_in,$token_out,$token_cache,$stop_reason" >> "$CSV"
done

echo "=== CSV 已生成: $CSV ==="
wc -l "$CSV"
echo ""
echo "得分汇总:"
echo "task_id,provider,score"
grep -v '^task_id' "$CSV" | awk -F',' '{printf "%-25s %-12s %s\n", $1, $2, $4}' | sort
