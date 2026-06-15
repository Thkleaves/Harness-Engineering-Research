# URL 短链接服务 过程分析：四轮对比

## 实验设计

| 维度 | 说明 |
|------|------|
| **项目** | URL 短链接服务（Spring Boot 3.4 + Java 21 + Maven） |
| **起始 Prompt** | A/B/C：`请从零构建一个URL短链接服务，支持创建短链接、302重定向和访问统计。` D：从裸Agent成品出发，修复缺陷 |
| **初始代码** | A/B/C：空壳项目（pom.xml + Application.java + application.yml） D：裸Agent 成品（8 文件，13 测试，3 API） |
| **模型** | DeepSeek-V4-pro |
| **权限模式** | `--permission-mode bypassPermissions`（D 为交互模式） |

### 四轮设计

| 轮次 | 名称 | CLAUDE.md | 用户参与 | 会话日志 |
|:---:|------|-----------|:---:|------|
| A | **裸Agent** | 3行：`完成用户的需求。直接实现全部功能...` | 无 | `69aceaf1-*.jsonl`（128KB） |
| B | **Harness自动** | 四阶段融合（需求分析→计划→TDD→Review），禁止提问 | 无 | `fb3921e4-*.jsonl`（177KB） |
| C | **Harness交互** | 四阶段融合，**允许提问并要求用户确认** | 3轮决策 | `56ca26e7-*.jsonl`（506KB） |
| D | **wow-harness** | SessionStart/End hooks + 16 skills + 治理框架，**允许提问** | 4轮决策 | `2d9048b7-*.jsonl`（257 events） |

---

## 1. 基本指标

| 指标 | 裸Agent | Harness自动 | Harness交互 | **wow-harness** |
|------|:---:|:---:|:---:|:---:|
| 耗时 | 131s（2.2min） | 175s（2.9min） | ~44min（含人工） | ~15min（交互） |
| 周转数 | 25 | 43 | ~60+ | ~70 |
| 输入 Token | 34,375 | 36,582 | 更高（含对话） | 更高（交互+治理） |
| 输出 Token | 5,974 | 8,945 | 更高（含对话） | 更高（spec+审查） |
| Java 文件 | 10 | 13 | **16** | **8（不增）** |
| 测试方法 | 13 | 17 | **19** | 17（13原有+4新增） |
| 编译 | ✅ | ✅ | ✅ | ✅ |
| 测试通过 | ✅ 13/0/0 | ✅ 17/0/0 | ✅ 19/0/0 | ✅ 17/0/0 |
| Agent提问 | 0次 | 0次（禁止） | **3轮** | **4轮** |
| **设计文档** | 无 | 无 | 无 | **spec.md** |

**Token 效率**（功能达成的单位成本）：

```
裸Agent:     34K in + 6K out = 40K total / 13 tests = 3,077 token/test
Harness自动: 37K in + 9K out = 46K total / 17 tests = 2,706 token/test
Harness交互: 对话开销较大，但用户输入减少了"自问自答"的无效 token
wow-harness: 交互+治理开销，但只修了3个缺陷（不像其他三轮全量重写），范围最小

---

## 2. 过程差异（从 turn 序列分析）

### A. 裸Agent — 一步到位（25 turns）

| Turns | 阶段 | 内容 |
|:---:|------|------|
| 1-2 | 扫项目 | ls 文件列表 |
| 3-9 | **一口气写所有代码** | model → repository → service → controller → DTOs |
| 10-17 | 写测试 | 2 个测试类（Controller 6 + Service 7） |
| 18-22 | 编译调试 | 修复 import、补 validation 依赖 |
| 23-25 | 验证 | mvn test，13 通过，输出总结 |

**特征**：无前导设计讨论，无中间检查点，无自审阶段。25 turns 中 20 个是写代码/测试，5 个是调试/验证。

### B. Harness自动 — 四阶段流水线（43 turns）

| Turns | 阶段 | 内容 |
|:---:|------|------|
| 1-4 | 扫项目+pom | 了解初始状态 |
| 5-12 | 🧠 **需求分析** | 自问自答：并发？幂等？存储？过期？自答"不幂等、Base62、内存" |
| 13-18 | 📋 **Spec+计划** | 定义 API 格式，选 Base62 自增方案，列 8 步实现计划 |
| 19-27 | 🔴 **TDD 红-绿** | 先写测试→跑红→写实现→跑绿 |
| 28-38 | 🔍 **Review** | 自审代码、补全局异常处理器 |
| 39-43 | ✅ **验证+输出** | mvn test 17 通过，结构化输出 API 文档 |

**特征**：严格按照四阶段，但所有决策都是 Agent 独白——选了 Base62 但没有问用户是否需要幂等，设计了统计但没有问用户需要什么粒度。

### C. Harness交互 — 用户驱动的四阶段（~60+ turns）

| Turns | 阶段 | 你的参与 |
|:---:|------|------|
| 1-4 | 扫项目 | — |
| 5-6 | 🧠 **提问1：短码策略** | **你选了"随机+24h幂等"** → Agent 推翻了自动版的核心假设 |
| 7-8 | 🧠 **提问2：存储/统计/过期** | **你定了"完整记录100条+永久有效"** |
| 9-10 | 📋 **提问3：API设计确认** | **你确认了端点设计** |
| 11 | 创建任务清单 | — |
| 12-50+ | 🔴🟢 **TDD 红-绿循环** | 观察（选 Allow 过权限） |
| 51-60+ | 🔍 **Review+修复** | Agent 自己发现 accessCount bug |
| 61+ | ✅ **最终验证** | 19 测试全绿 |

**特征**：用户在前 10 turns 完成了"需求注入"——Agent 知道要随机码、24h幂等、完整统计——然后 TDD 阶段自动执行。跟你的交互集中在前面 20%，80% 的编码工作不需要你参与。

### D. wow-harness — 诊断驱动的最小修复（~70 turns）

| Turns | 阶段 | 内容 |
|:---:|------|------|
| 1-2 | SessionStart | hooks 加载治理上下文、16 skills 列表 |
| 3-4 | 扫项目 | ls 文件、mvn test 确认 13 测试全过 |
| 5-12 | 🧠 **提问1：技术栈** | A(Spring Boot) / B(Javalin) / C(其他语言)，用户选 A |
| 13-18 | 🧠 **提问2：持久化** | A(JPA+H2) / B(PostgreSQL) / C(内存)，用户选 C |
| 19-24 | 🧠 **提问3：范围** | A(只修缺陷+补测试) / B(自定义短码) / C(过期+删除)，用户选 A |
| 25-30 | 📋 **诊断+设计方案** | 分析 3 个缺陷：碰撞无重试、URL 无校验、统计非原子。出 spec |
| 31-32 | 📋 **提问4：确认方案** | 用户确认 D1-D4 方案，Agent 写 spec 到磁盘 |
| 33-55 | 🔴🟢 **TDD 红-绿** | 先写测试→跑红→修缺陷→跑绿 |
| 56-65 | 🔍 **Review+验证** | mvn test 17/17 全过，总结改动 |
| 66-70 | SessionEnd | hooks 反思总结，持久化进度 |

**特征**：wow-harness 没有重写项目——它先诊断现有代码的缺陷，写 spec，让用户确认，然后精准修 3 个缺陷。和 C（全量重写）的哲学完全相反：**最小范围，最大确定性。** 4 轮提问覆盖了技术栈、持久化、范围、方案确认——比 C 多了一轮范围确认。

---

## 3. 架构决策对比

| 决策点 | 裸Agent | Harness自动 | Harness交互 | **wow-harness** |
|--------|:---:|:---:|:---:|:---:|
| **短码算法** | Base62 自增ID | Base62 自增ID | **随机7位 SecureRandom** | 随机7位（沿用 Bare 已有） |
| **幂等策略** | 无 | 无 | **24h urlIndex 去重** | 无（范围未涉及） |
| **存储方案** | ConcurrentHashMap + Repository | ConcurrentHashMap + UrlStore | **ConcurrentHashMap × 2**（store + urlIndex） | ConcurrentHashMap + Repository（沿用） |
| **统计粒度** | AtomicLong 仅计数 | timestamp + UA 记录 | **timestamp + Referer + UA，100条** | AtomicLong 计数（沿用） |
| **时钟注入** | 无 | 无 | **Clock 注入** | 无（范围未涉及） |
| **短码生成器** | 内嵌 private 方法 | 独立 Base62Encoder 类 | **接口抽象 + RandomShortCodeGenerator 实现** | 内嵌 private 方法（沿用） |
| **异常处理** | Controller 内嵌 | 独立 @RestControllerAdvice | **@RestControllerAdvice + 自定义异常** | Controller 内嵌 + 503 映射（沿用+增强） |
| **URL 校验** | @NotBlank | @NotBlank + @Valid | **协议检查 + URI 解析 + 规范化** | **@NotBlank + @URL**（增量修复） |
| **碰撞处理** | 无 | 无 | 10次重试 → 429 | **3次重试 → 503**（增量修复） |
| **DTO 风格** | 普通 POJO | Record | Record | 普通 POJO（沿用） |
| **URL 构建** | 硬编码 localhost | 动态 scheme+host+port | 动态 scheme+host+port | 硬盘编码（沿用） |

**关键观察**：wow-harness 不从零重写——它在现有代码上做增量修复。和 A/B/C 的全量重写思路完全不同。缺点是没触及核心架构（幂等、Clock），优点是零过设计——只改了 3 个文件。

---

## 4. 测试策略对比

| 维度 | 裸Agent | Harness自动 | Harness交互 | **wow-harness** |
|------|:---:|:---:|:---:|:---:|
| 测试层次 | 集成 + 单元 | 集成 + 单元 | 集成 + 单元 | 集成 + 单元 |
| 测试文件数 | 2 | 3 | 3 | 2 |
| **总测试数** | **13** | **17** | **19** | **17**（13原有+4新增） |
| Controller 集成 | 6 | 7 | 7 | 7（+非法URL） |
| Service 单元 | 7 | — | 8 | 10（+碰撞重试×2+字符集） |
| Base62/编码器专项 | — | 4 | — | — |
| ShortCodeGenerator | — | — | 4 | — |
| UrlStore 单元 | — | 5 | — | — |
| **24h 幂等测试** | — | — | ✅ | —（范围外） |
| **碰撞重试测试** | — | — | ✅ | **✅（3次→503）** |
| **100条上限测试** | — | ✅ | ✅ | —（沿用原有） |
| **Clock 时间推进** | — | — | ✅ | —（范围外） |
| **URL 校验边界** | — | — | ✅（协议/空/null） | **✅（@URL拒绝非法格式）** |

**关键观察**：wow-harness 的测试策略是"给缺陷补测试"——碰撞重试、URL 校验各一个测试。不做额外覆盖。和裸 Agent 比（0 个缺陷测试），和交互版比（19 个全量测试），它是"刚好够"的路线。

---

## 5. 代码质量对比

| 维度 | 裸Agent | Harness自动 | Harness交互 | **wow-harness** |
|------|:--:|:--:|:--:|:--:|
| 单一职责 | ⚠️ 编码内嵌 Service | ✅ 编码器独立 | ✅ 接口+实现 | ✅ 保持原有结构 |
| Repository 模式 | ✅ | ❌ Service 直接持有 Map | ❌ Service 直接持有 Map | ✅ 保持原有 Repository |
| 异常处理解耦 | ❌ Controller 内嵌 | ✅ 独立 Handler | ✅ 独立 Handler | ⚠️ Controller 内嵌+503 |
| 配置外化 | ✅ @Value | ❌ | ❌（动态构建替代） | ✅ @Value（沿用） |
| DTO 不可变性 | ❌ 普通类 | ✅ Record | ✅ Record | ❌ 普通类（沿用） |
| 时间依赖管理 | ❌ Instant.now() | ❌ Instant.now() | ✅ **Clock 注入** | ❌（范围外） |
| 碰撞处理 | — | — | ✅ 10次重试 → 429 | **✅ 3次重试 → 503** |
| URL 规范化 | ❌ | ❌ | ✅ normalizeUrl + URI 解析 | **⚠️ @URL 注解校验** |
| **设计文档** | ❌ | ❌ | ❌ | **✅ spec.md 写入磁盘** |

---

## 6. 你的每次互动如何改变了代码

### C. Harness 交互

| 你的回答 | 直接驱动的实现 |
|----------|---------------|
| **"选2，随机短码。24h内重复返回已有"** | `SecureRandom` + `urlIndex` ConcurrentHashMap + `DEDUP_WINDOW` 24h + `isReused()` 标记 |
| **"完整记录（时间戳、Referer、User-Agent），100条"** | `AccessRecord(time, referer, ua)` + `ConcurrentLinkedDeque` + `while(size>100) pollFirst()` |
| **"7位随机，可以"** | `CHARS = "a-z+A-Z+0-9"` + `CODE_LENGTH = 7` |
| **"10次碰撞返回429，可以"** | `MAX_RETRIES = 10` → `RuntimeException` → `@ExceptionHandler` → 429 |
| **"按 TDD 先测试再实现"** | 严格执行红-绿-重构，3 个测试类 19 个方法 |

### D. wow-harness

| 你的回答 | 直接驱动的实现 |
|----------|---------------|
| **"A，保留 Spring Boot"** | 不改变技术栈，在现有代码上修改 |
| **"C，保持纯内存"** | 不引入 JPA/H2，沿用 ConcurrentHashMap |
| **"A，只修缺陷+补测试"** | 范围锁定在 3 个缺陷，不加新功能 |
| **"可以，继续推进"** | 确认 spec，Agent 进入 TDD 实现 |

**和 C 的关键差异**：C 的决策改变了架构（随机 vs 自增、幂等 vs 不幂等），D 的决策锁定了范围（不换栈、不加功能、只修缺陷）。C 的交互是"你要什么样的产品"，D 的交互是"你要修到什么程度"。

**对应到具体文件**：

| 需求 | 文件 | 改动 |
|------|------|------|
| 碰撞重试 | `UrlService.java:30-38` | `for(i<3)` + `findByShortCode` 检查 + 503 |
| URL 校验 | `CreateUrlRequest.java` | `@NotBlank` → `@NotBlank + @URL` |
| 碰撞测试 | `UrlServiceTest.java` | +3 测试（重试成功/超限/字符集） |
| URL 测试 | `UrlControllerTest.java` | +1 测试（非法 URL→400） |

---

## 7. 核心结论

### 7.1 四轮 Harness 的价值定位

| 维度 | A.裸Agent | B.Harness自动 | C.Harness交互 | **D.wow-harness** |
|------|:--:|:--:|:--:|:--:|
| 代码结构化 | 基线 | +20% | +30% | **不改结构，只修缺陷** |
| 测试覆盖 | 13 | 17 | 19 | **17（补4个缺陷测试）** |
| 测试质量 | 基础 | 多但泛 | 精准匹配需求 | **精准匹配缺陷** |
| 核心算法选择 | Base62自增 | 与A一致 | **完全不同** | **沿用A的算法** |
| 过设计/欠设计 | 欠设计 | 过设计 | **刚好** | **刚好** |
| **产出** | 可跑的代码 | 模块化代码 | 用户想要的代码 | **有 spec 的修复** |

### 7.2 交互的关键差异

1. **架构决策质量**：自动 Harness 的自问自答无法替代真实需求输入。C 和 D 都通过用户交互改变了输出——C 改变了架构方向，D 锁定了修复范围

2. **wow-harness 的独特价值**：四轮中唯一产出 spec 文档的。**SessionStart hooks 迫使 Agent 在写代码前先诊断、写方案、让用户确认。** 这和 C 的 brainstorming 不同——C 问的是"你要什么"，D 问的是"现在有什么问题"

3. **过设计风险为四轮最低**：A 欠设计（无碰撞处理），B 过设计（无用的 Base62Encoder），C 全量重写。**D 只改了 3 个文件 —— 每个改动都有 spec 背书**

4. **wow-harness 的局限**：只修了用户批准的缺陷。幂等策略、Clock 注入、完整访问日志——这些在 C 中进化为核心亮点的东西，在 D 中根本没有触及。因为用户选了"只修缺陷"

### 7.3 对 Harness 评测框架的启示

1. **禁止用户交互的自动评测衡量的是"Agent 猜测用户需求的能力"，不是"Harness 引导协作的能力"**
2. 真实场景中 Harness 的价值体现在前 20% 的需求澄清阶段——用户参与决策避免后续返工
3. **wow-harness 证明了"先诊断后修复"路线的可行性**——不重写整个项目，精准修缺陷，每个改动有 spec 可追溯
4. **不同的 Harness 适应不同的工作模式**：Superpowers/Gstack/OpenSpec 适合从零构建，wow-harness 适合存量代码修复

---

## 8. 数据溯源

| 数据 | 位置 |
|------|------|
| 裸Agent 源码 | `trials/url-shortener-bare/src/` |
| Harness自动 源码 | `trials/url-shortener-harness/src/`（第一次跑的） |
| Harness交互 源码 | `trials/url-shortener-harness/src/`（覆盖后的） |
| **wow-harness 源码** | `trials/url-shortener-wow/src/` |
| 裸Agent 会话日志 | `~/.claude/projects/d--Work-kleaves-Harness-trials-url-shortener-bare/69aceaf1-*.jsonl` |
| Harness自动 会话日志 | `~/.claude/projects/d--Work-kleaves-Harness-trials-url-shortener-harness/fb3921e4-*.jsonl` |
| Harness交互 会话日志 | `~/.claude/projects/d--Work-kleaves-Harness-trials-url-shortener-harness/56ca26e7-*.jsonl` |
| **wow-harness 会话日志** | `~/.claude/projects/D--Work-kleaves-Harness-trials-url-shortener-wow/2d9048b7-*.jsonl` |
| 裸Agent 输出 log | `trials/url-shortener-bare-output.log` |
| **wow-harness spec** | `trials/url-shortener-wow/docs/superpowers/specs/2026-06-15-url-shortener-defects-design.md` |
