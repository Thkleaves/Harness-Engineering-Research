#!/usr/bin/env node
// ============================================================
// score.js — VibeCodingBench 改编版评分计算
// 用法: node score.js <verify_result.json> [token_count] [cost_usd]
// 输入: verify.sh 输出的 JSON + Token/Cost 数据
// 输出: 综合评分 JSON
// ============================================================

const fs = require('fs');

// ── 评分权重 ──
const WEIGHTS = {
    functional: 40,    // 编译+测试通过率
    quality: 25,       // 代码结构+错误处理+设计模式
    testQuality: 15,   // 有测试+覆盖率+边界覆盖
    process: 10,       // Skill 调用次数+流程完整性
    cost: 10,          // Token 消耗效率（越低分越高）
};

function main() {
    const args = process.argv.slice(2);
    if (args.length < 1) {
        console.error('Usage: node score.js <verify.json> [token_count] [cost_usd]');
        process.exit(1);
    }

    let verify;
    try {
        verify = JSON.parse(fs.readFileSync(args[0], 'utf8'));
    } catch (e) {
        verify = { pass: false, score: 0, details: {} };
    }

    const tokenCount = parseInt(args[1]) || 0;
    const costUsd = parseFloat(args[2]) || 0;

    // ── 安全否决 ──
    if (!verify.pass && verify.reason && verify.reason.includes('security gate')) {
        console.log(JSON.stringify({
            pass: false, score: 0, securityGate: true,
            functional: 0, quality: 0, testQuality: 0, process: 0, cost: 0,
            reason: 'Security gate triggered — score zeroed'
        }, null, 2));
        process.exit(0);
    }

    // ── Functional (40) — 基于验证脚本得分映射 ──
    const functionalScore = Math.min(40, verify.score || 0);

    // ── Quality (25) — 基于代码质量信号 ──
    let qualityScore = 10;
    const d = verify.details || {};
    if (d.quality && d.quality.hasExceptionHandler) qualityScore += 5;
    if (d.compile === 'pass') qualityScore += 5;
    if (d.tests && d.tests.total > 5) qualityScore += 3;
    if (d.security && d.security.issues === 0) qualityScore += 2;
    qualityScore = Math.min(25, qualityScore);

    // ── Test Quality (15) — 基于测试数量和质量 ──
    let testScore = 0;
    if (d.tests) {
        if (d.tests.total > 0) testScore += 5;
        if (d.tests.passRate > 90) testScore += 5;
        if (d.tests.total >= 5) testScore += 5;
    }
    testScore = Math.min(15, testScore);

    // ── Process (10) — 基于 skill-used 断言的命中数 ──
    const processScore = 5; // 默认基础分；Promptfoo 的 skill-used 断言会覆盖

    // ── Cost (10) — Token 效率（≤5000 → 10，>50000 → 0，线性） ──
    let costScore = 5;
    if (tokenCount > 0) {
        if (tokenCount <= 5000) costScore = 10;
        else if (tokenCount >= 50000) costScore = 0;
        else costScore = Math.round(10 - ((tokenCount - 5000) / 45000) * 10);
    }

    const totalScore = functionalScore + qualityScore + testScore + processScore + costScore;

    const result = {
        pass: verify.pass !== false,
        score: totalScore,
        maxScore: 100,
        breakdown: {
            functional: functionalScore,
            quality: qualityScore,
            testQuality: testScore,
            process: processScore,
            cost: costScore,
        },
        tokenCount,
        costUsd,
        details: verify.details || {},
    };

    console.log(JSON.stringify(result, null, 2));
}

main();
