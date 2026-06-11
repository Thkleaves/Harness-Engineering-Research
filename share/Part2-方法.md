# Part 2：方法 —— 12 任务 × 4 组对照实验设计

---

## 2.1 评测对象（4 组，同一模型）

| 组别 | 说明 | 核心特征 |
|:----:|------|----------|
| **A. Baseline** | 裸 DeepSeek V4 Pro，无任何 Skill 注入 | 拿到 prompt → 直接写代码 |
| **B. Superpowers** | 注入 14 个技能 | brainstorming → plan → TDD → review → verify |
| **C. Gstack** | 注入角色分工技能 | /ceo 定需求 → /engineer 实现 → /qa 测试 → /devops 配置 |
| **D. OpenSpec** | 注入 Spec 驱动工作流 | 先写 Spec 定义 API → 再按 Spec 实现 |

**所有组使用相同模型（DeepSeek-V4-pro），相同任务 prompt，相同工作区代码基线。**

唯一的变量是 **Harness / 技能注入策略**。

---

## 2.2 12 个 Spring Boot 任务（四级难度）

```
L1 简单 (1-2步)
  ① 参数校验      为 UserCreateRequest DTO 添加 Jakarta Validation
  ② Actuator     添加 Health + Metrics + Readiness 端点

L2 中等 (3-5步)
  ③ 分页排序      列表 API 加分页参数 + 排序白名单防注入
  ④ 注册验证      用户注册 + 邮箱 Token 验证 + 状态机
  ⑤ Redis 缓存    @Cacheable/@CacheEvict + TTL + Mock 测试
  ⑥ 限流          手写 Token Bucket 算法 + 线程安全 + 并发测试

L3 复杂 (5-10步)
  ⑦ JWT 认证      register/login/refresh/logout + 黑名单 + 角色权限
  ⑧ 三层重构      Controller → Service → Repository 抽取，保持 API 不变
  ⑨ 文件上传      CSV 上传 + @Async 解析 + 状态查询 + 失败重试
  ⑩ N+1 修复      @EntityGraph 优化 + SQL 次数验证 + 回归测试

L4 高级 (10+步)
  ⑪ 并发预订      @Version 乐观锁 + 冲突重试 + 100 线程无超卖
  ⑫ Flaky 测试    时区 bug 修复 + Clock 注入 + 参数化多时区测试
```

每个任务都是一个独立的 Spring Boot 项目（`eval/workspaces/01-validation/` 等），包含已有的代码骨架。Agent 需要在已有代码基础上增量开发。

---

## 2.3 评分体系（VibeCodingBench 改编版）

```
┌────────────────────────────────────────────────────┐
│                                                    │
│  Functional    40%  ← 编译 + 测试通过率              │
│  Quality       25%  ← 代码结构 + 错误处理 + 设计模式  │
│  Test Quality  15%  ← 测试存在？覆盖率？边界覆盖？    │
│  Process       10%  ← Skill 调用 + 步骤合理性        │
│  Cost          10%  ← Token 消耗（越低分越高）        │
│                                                    │
│  Security     Gate  ← 一票否决（硬编码/注入/越权）     │
│                                                    │
└────────────────────────────────────────────────────┘
```

### 否决规则

```
任何任务出现以下情况 → 该任务得分归零:
  1. 引入安全漏洞（硬编码密钥、SQL 注入、越权）
  2. 破坏现有功能（回归测试失败）
  3. 编译不通过
```

---

## 2.4 自动化评测流程

```
每个任务 × 4 个 Provider, 独立运行:

1. Promptfoo 将 task prompt 发送给 Provider
2. Provider（Claude Agent SDK）在独立 worktree 中执行
3. Agent 完成编码后退出
4. Promptfoo 调用 verify.sh:
     ├─ mvn compile ──→ 编译通过？
     ├─ mvn test    ──→ 解析 surefire 报告（total/passed/failures）
     ├─ 安全检查     ──→ Semgrep 扫描
     └─ 代码质量     ──→ LLM-rubric 评分
5. 输出 JSON: { pass, score, compile, tests, taskScore, qualityScore }
6. 汇总到 CSV
```

### 运行命令

```bash
cd eval
npx promptfoo eval --config promptfooconfig.yaml --max-concurrency 4
```

---

## 2.5 评测规模

| 指标 | 数值 |
|------|:---:|
| 任务数 | 12 |
| 对比组 | 4（Baseline / Superpowers / Gstack / OpenSpec） |
| 每组每任务运行次数 | 1 |
| 有效评测记录 | **48 条** |
| 模型 | DeepSeek-V4-pro |
| 运行日期 | 2026-06-10 |

---

## 2.6 每条记录包含的数据

| 字段 | 含义 |
|------|------|
| `task_id` | 任务编号 |
| `provider` | 评测组别 |
| `score` | 综合得分（0-100） |
| `duration_sec` | 运行耗时 |
| `token_in` | 输入 Token |
| `token_out` | 输出 Token |
| `token_cache` | 缓存命中的 Token |
| `stop_reason` | 终止原因 |

完整数据集在 `eval/results/eval-results.csv`。

---

## 2.7 过程评测补充：自动评测测不到的维度

12 任务评测只能看**结果分数**，看不到**过程差异**——Agent 怎么做决策、有没有提问、架构选择是否正确。

我们补了一轮**过程评测**：同一个 URL 短链接服务项目，三轮对比——

| 轮次 | 名称 | 用户参与 |
|:---:|------|:---:|
| A | 裸 Agent | 无 |
| B | Harness 自动（禁止提问） | 无 |
| C | **Harness 交互（允许提问）** | **3 轮需求澄清** |

通过对比 A vs B 看"Harness 流程本身的增量"，B vs C 看"用户交互的增量"。日志从 Claude 会话 JSONL 中逐 turn 提取分析。详见 `eval/results/process-analysis-url-shortener.md`。

---

> 评测配置：`eval/promptfooconfig.yaml` | 评分脚本：`eval/scripts/verify.sh` | 过程分析：`eval/results/process-analysis-url-shortener.md`
> 相关规划文档：`plans/` 目录（06-评测方案设计.md 等 6 篇）
