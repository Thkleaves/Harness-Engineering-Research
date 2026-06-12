# harnessforge inspect 结果

**日期**：2026-06-09
**工作空间**：04-book-api-forge
**命令**：`harnessforge inspect`

---

## 检测摘要

```
--------------------- harness inspect @ 04-book-api-forge ---------------------
 languages     java
 frameworks    无
 package mgrs  无
 tests         无
 lint          无
 build         无
 ci            无
 container     无
 git           无
 env vars      无
 existing      .claude/CLAUDE.md, .cursor/rules, AGENTS.md,
               .continue/config.json, .windsurf/rules
 MCPs found    无
 files         31
 lines (~)     2007
```

---

## 字段说明

| 字段 | 值 | 说明 |
|------|-----|------|
| languages | java | 项目主要编程语言 |
| frameworks | 无 | 未检测到 Spring 等框架（实际上用了 Spring Boot，但未自动识别） |
| package mgrs | 无 | 未检测到包管理器 |
| tests | 无 | 未配置测试命令 |
| lint | 无 | 未配置 lint 命令 |
| build | 无 | 未配置构建命令（实际用 Maven，pom.xml 存在） |
| ci | 无 | 未启用 CI |
| container | 无 | 无 Dockerfile 或 docker-compose |
| git | 无 | 无远程仓库 |
| env vars | 无 | 无预期环境变量 |
| existing agent configs | 5 个 | 检测到 Claude Code、Cursor、Continue、Windsurf 和通用 AGENTS.md 配置 |
| MCPs found | 无 | 未检测到 MCP 服务器 |
| files | 31 | 项目文件总数 |
| lines (~) | 2007 | 估计代码总行数 |

---

## 已有 Agent 配置

- `.claude/CLAUDE.md`
- `.cursor/rules`
- `AGENTS.md`
- `.continue/config.json`
- `.windsurf/rules`

## 备注

- 项目实际使用 **Spring Boot + Maven**，但 `harnessforge inspect` 未自动识别出框架和构建工具
- `profile.yaml` 中 `frameworks: []`、`build_command: null` 也反映了这一点
