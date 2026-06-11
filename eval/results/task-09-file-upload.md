# 任务⑨ 文件上传 评测总结

## 任务要求
实现 CSV 文件上传+异步处理：POST /api/files/upload、@Async 后台解析入库、状态查询、10MB 大小限制、失败重试 3 次指数退避、坏行记录到 errors 文件。

## 得分对比

| Provider | Score | Duration | Token(in/out) |
|----------|:-----:|:--------:|---------------|
| Baseline | 95 | 662s | 114.3K / 29.7K |
| Superpowers | 95 | 607s | 69.9K / 31.7K |
| Gstack | 95 | 834s | 56.8K / 38.2K |
| OpenSpec | 95 | 506s | 56.4K / 25.2K |

## 分析

四组全部 95 分，完全一致。文件上传任务涉及多个技术点（MultipartFile、@Async、TaskExecutor、状态机、重试、CSV 解析），各组均覆盖到位。丢失的 5 分是代码质量中缺少 input validation 或 exception handling 的部分。

- **OpenSpec 最快**（8.4min）：Spec 定义了上传接口和状态转换，实现效率高
- **Gstack 输出 token 最高**（38K）：各角色讨论产出了更详细的代码
- **Baseline 输入 token 最高**（114K）：裸 Agent 可能更多依赖上下文重新理解需求

## 结论
多技术点组合的任务上，四组表现拉不开差距。任务足够复杂导致各组都需要覆盖全部点，Harness 的流程引导在此场景下没有明显的增益或损失。
