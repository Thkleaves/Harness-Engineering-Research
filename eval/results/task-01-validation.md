# 任务① 参数校验 评测总结

## 任务要求
为 UserCreateRequest DTO 添加 Jakarta Validation 注解（username/email/password/age），Controller 加 @Valid，全局异常处理返回 400，编写测试。

## 得分对比

| Provider | Score | Duration | Token(in/out) |
|----------|:-----:|:--------:|---------------|
| Baseline | 95 | 411s | 53.9K / 16.0K |
| Superpowers | 95 | 456s | 88.6K / 20.7K |
| Gstack | 95 | 213s | 45.2K / 8.3K |
| OpenSpec | 95 | 184s | 43.0K / 7.5K |

## 分析

四组得分完全一致（95/100）。任务简单明确，要求清晰无歧义，各组都完成了 @Valid、@NotBlank/@Email/@Size、GlobalExceptionHandler 和测试。

差异在于效率：
- **OpenSpec & Gstack** 用时最短（~3min）、token 最低（~50K），说明 Spec 驱动或角色分工流程在简单任务上不会过度准备
- **Superpowers** 用时最长（7.6min）、token 最高（109K），brainstorming 6 轮 + plan + TDD + review 全流程在简单任务上产生了不必要的流程开销
- **Baseline** 裸 Agent 直接动手，效率中等

## 结论
L1 简单任务上，Harness 的流程牵引**没有带来得分提升**，反而增加了 token 消耗。裸 Agent 在没有指导的情况下同样能正确完成明确的需求。
