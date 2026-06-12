# PLAN — 冲刺执行计划（实际执行记录）

> **目标**: 产出一场有数据支撑的 Harness Engineering 团队技术分享
> **时间**: 3 个工作日（2026-06-09 ~ 2026-06-11）+ 1 天收尾（2026-06-12）
> **实际规模**: 12 任务 × 4 Provider × 1 次运行 = 48 条记录（受限于单终端串行执行时间）
> **核心原则**: 先跑通再规模化、全量评测用数据说话、最终交付是分享文档

---

## 📅 全景

```
Day 1                  Day 2                  Day 3                  Day 4
工具深潜 + 亲手跑通     评测基础设施 + L1+L2    L3+L4 全量 + 过程评测   可视化 + 文档收尾
                                             + 分享文档
```

---

## Day 1 — 工具深潜，亲手跑通 ✅

**目标**: 建立亲身感受，不追求数据，追求理解。

| # | 任务 | 实际执行 |
|---|------|------|
| 1 | 速读 01-概念与背景 + 02-工具调研与对比 | 完成 |
| 2 | 装好三个工具 | Superpowers / agent-harness-kit / HarnessForge 安装完成 |
| 3 | Superpowers 跑 L1（DTO Validation）完整流程 | 完成。brainstorming 6 轮 → 三选一 → TDD → review，发现 @Digits 与 Double 不兼容 |
| 4 | 同一功能裸 Agent 对比 | 完成。架构一致，但 Superpowers 校验深度更深（ISBN/PriceFormat/PUT 部分更新） |
| 5 | agent-harness-kit 10 维度评审 | 完成。12 个问题（严重3/中等4/轻微5），覆盖 Double 精度、save() 副作用等 |
| 6 | HarnessForge init 试用 | 完成。~20 个配置文件，发现 inspect 框架检测漏检 Spring Boot |
| 7 | Superpowers 跑 L2（分页排序） | 完成。brainstorming 5 轮 → TDD 23 测试，遇到子代理 DeepSeek API 不兼容 |
| 8 | 整理踩坑清单 | 完成。11 条（阻塞2/阻碍5/轻度4） |

**检查点**:
- [x] Superpowers 全流程跑通 ≥1 次
- [x] 裸 Agent vs Superpowers 对比感受已记录
- [x] agent-harness-kit 10 维度评审跑通 ≥1 次
- [x] HarnessForge init 已试用
- [x] 踩坑清单 11 条

---

## Day 2 — 评测基础设施 + Pilot + L1/L2 全量跑 ✅

> **策略调整**: 单终端串行执行，各任务只跑 1 次（而非原计划的多终端并行 × 3 次重复），
> 释放时间给 L3/L4 和过程评测。

**目标**: 搭好框架，跑通 Pilot，完成 L1+L2，修复工具链问题。

| # | 任务 | 实际执行 |
|---|------|------|
| 1 | 处理 Day 1 踩坑清单 | CRLF 修复、.gitattributes、mvn PATH 问题 |
| 2 | 搭建评测环境 | 12 个 workspace + run-eval.sh + verify.sh + promptfoo 配置 |
| 3 | Pilot: 任务③ 分页排序 | 4 组 × 1 次，验证全链路通过 |
| 4 | 修复 CRLF 导致全任务 score=0 | Git CRLF→LF 破坏 bash 重定向，sed 批量修复 + .gitattributes 锁定 |
| 5 | L1 批量跑: ①② | 4 组 × 2 任务 × 1 次 = 8 条 |
| 6 | L2 批量跑: ③④⑤⑥ | 4 组 × 4 任务 × 1 次 = 16 条，分 l2a/l2b 两终端并行 |
| 7 | verify.sh 加固诊断 | 新增 maven stderr 捕获、多路径 mvn 探测 |
| 8 | 编写 rebuild-csv.sh | 批量重验 + token 提取 + CSV 重建脚本 |

**检查点**:
- [x] CRLF 问题修复，eval 脚本全部可运行
- [x] Pilot 通过（4/4 全链路跑通）
- [x] L1: 8 条
- [x] L2: 16 条
- [x] 累计: 26 条（含任务⑩⑪单独测试）

---

## Day 3 — L3/L4 全量 + 过程评测 + 分享文档 ✅

> **实际执行**: 跑完 L3+L4 全量，补充过程评测（URL 短链接三轮对比），
> 编写逐任务分析 MD 和分享文档 Part1-4。

**目标**: 跑完所有任务，汇总数据，产出一套完整的分析文档和分享材料。

| # | 任务 | 实际执行 |
|---|------|------|
| 1 | L3 批量跑: ⑦⑧⑨⑩ | 4 组 × 4 任务 × 1 次 = 16 条 |
| 2 | L4 批量跑: ⑪⑫ | 4 组 × 2 任务 × 1 次 = 8 条，补齐后共 48 条 |
| 3 | 全局得分表汇总 | Gstack 1090 > OpenSpec 1065 > Baseline = Superpowers 1045 |
| 4 | 12 篇逐任务分析 MD | task-01 ~ task-12，每篇分析各组差异和失败模式 |
| 5 | 过程评测: URL 短链接三轮对比 | 裸Agent vs Harness自动(禁止交互) vs Harness交互(允许3轮提问) |
| 6 | 过程评测核心发现 | 自动 Harness 与裸Agent 架构选型完全一致；用户交互彻底改变核心决策 |
| 7 | 分享文档 Part1-4 | 问题→方法→数据→总结，多轮打磨去除指导性语言 |
| 8 | README.md 更新 | 覆盖 Day 1-3 全量内容 |

**检查点**:
- [x] 48 条完整数据
- [x] Gstack 总分第一（1090），Superpowers = Baseline（1045）
- [x] 12 篇任务分析 MD
- [x] 过程评测三轮对比完成
- [x] 分享文档 Part1-4 定稿

---

## Day 4 — 数据可视化 + 文档收尾 ✅

> **实际执行**: 生成可视化图表并嵌入分享文档，整理目录结构。
> PPT/Slide 制作用 Markdown 分享文档替代。

**目标**: 把数据变成可视化图表，完善仓库结构。

| # | 任务 | 实际执行 |
|---|------|------|
| 1 | Python matplotlib 出 4 张图 | 柱状图(总分对比)、趋势线(L1→L4)、散点图(Token效率)、热力图(12×4) |
| 2 | 图表嵌入分享文档 | Part3 嵌入全部 4 张、Part4 引用趋势图 |
| 3 | 目录整理 | records/plans/share 合并为 docs/plans/records/share，workspaces/ 重命名为 trials/ |
| 4 | plans 文档修正 | 将原始计划文档对齐实际执行结果 |

**检查点**:
- [x] 4 张图表 PNG 生成
- [x] 图表嵌入 Markdown 分享文档
- [x] 目录结构整理完成
- [x] plans 文档更新为实际执行记录

---

## 🎯 实际产出清单

| 产出 | 格式 | 说明 |
|------|------|------|
| 全量评测数据 48 条 | CSV | eval/results/eval-results.csv |
| 评测总结报告 | Markdown | eval/results/README.md |
| 逐任务分析 12 篇 | Markdown | eval/results/task-01 ~ task-12 |
| 过程评测分析 | Markdown | eval/results/process-analysis-url-shortener.md |
| 4 张可视化图表 | PNG | eval/results/charts/ (柱状/趋势/散点/热力图) |
| 分享文档 4 篇 | Markdown | docs/share/Part1-问题 ~ Part4-总结 |
| Day 1 实操记录 6 篇 | Markdown | docs/records/ |
| 调研规划文档 9 篇 | Markdown | docs/plans/ |

---

## 📐 实际分享结构（对应 docs/share/Part1-4）

```
Part1-问题: AI 编码的瓶颈从模型转向工程化
  - 两则翻车案例（Book API 参数校验 + URL 短链接裸 Agent）
  - 核心论点: Agent = 模型 × Harness

Part2-方法: 12 任务 × 4 组对照实验
  - 4 组 Provider: Baseline / Superpowers / Gstack / OpenSpec
  - 12 个 Spring Boot 任务 L1→L4
  - 评分体系 + 自动化流程 + 过程评测补充

Part3-数据: 10 个关键发现
  - 总分排名: Gstack 1090 > OpenSpec 1065 > Baseline = Superpowers 1045
  - 过程评测: 禁止交互 = 剥夺 Harness 核心价值
  - 4 张图表（柱状/趋势/散点/热力图）

Part4-总结: 不同场景下各组表现差异
  - 算法类 Superpowers 最强，API 设计 OpenSpec 占优，多组件 Gstack 领先
  - 所有 Harness 都怕全局重构（任务⑫ 四组 70 分）
```

---

## ⚠️ 实际踩坑与教训

1. CRLF 行尾符导致全任务 score=0 — Git on Windows 默认行为，.gitattributes 锁定 LF 解决
2. WSL 非交互 shell 下 mvn 找不到 — 需在 verify.sh 中多路径探测
3. 子代理与 DeepSeek API 不兼容 — `reasoning_effort` 参数冲突，降级为 Inline Execution
4. 单终端串行执行无法达到 144 条 — 48 条已足够支撑结论
5. 自动评测中禁止 AskUserQuestion 系统性低估了 Harness 价值 — 过程评测证实
6. 所有 Agent 倾向"加新代码"而非"改旧代码" — 任务⑫ 四组全败于此
