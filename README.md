# GTCEu ATM10 Standalone Merge

这是 All the Mods 10 / Minecraft 1.21.1 使用的单包 GregTech CEu 构建。

本发布包把 GTNA（GregTech: Nexus Addon）和 Programmed Circuit Card 的代码、资源、配方与兼容层嵌入 GTCEu 容器中。运行时只需要安装这一个 GTCEu JAR，不需要另外安装这两个附属模组的 JAR。

## 下载

将 `release/gtceu-1.21.1-8.0.1-atm10.5-with-gtna-pccard-recipefix-v6.jar` 放入 NeoForge 1.21.1 的 `mods` 目录。

SHA-256:

`BF2888E67BED0E447482E8EEE8E8AAC2363AE795C0976E8997CAF05005BCF136`

## 验证结果

- NeoForge: `21.1.248`
- Minecraft: `1.21.1`
- GTCEu 容器版本: `8.0.1-atm10.5`
- 成品逻辑模组容器: `gtceu`（没有独立 `gtna` / `pccard` 模组入口）
- GTNA 配方审计: `561/561`
- 编程电路物品审计: `5/5`
- JEI 清理审计: `115` 个候选，移除 `114` 个，缺席 `1` 个
- 空材料配方审计: `224/224` 有效，`0` 个无效或未知材料对
- 最终单包实机启动: 通过；致命错误 `0`；OOM `0`

完整审计报告位于 `release/`。报告中的本机路径只用于记录生成环境，不是运行时依赖。

## 构建

需要 JDK 21、Gradle 8.14、NeoForge 1.21.1 依赖，以及 ATM10 对应的 GTCEu、LDLib、AE2、GuideME、AdvancedAE、JEI 等依赖。源代码位于 `source/gtna` 和 `source/pccard`；合并脚本位于 `tools/`。

构建两个源项目后，使用 `tools/merge_into_gt.py` 将它们和桥接 Mixin 嵌入基础 GTCEu JAR，再执行 `tools/audit_release.py` 与 `tools/audit_localization.py`。脚本会校验 ZIP CRC、重复路径、Mixin 类、NeoForge 依赖、语言键和嵌入模块，遇到未声明冲突会直接失败。

## 许可与来源

这是面向 ATM10 的非官方维护构建，不代表 GregTechCEu、All The Mods 或两个上游附属模组作者。请同时遵守上游项目的许可证和署名要求：GTCEu、GTNA、Programmed Circuit Card、LDLib 以及成品 JAR 中包含的第三方库均保留各自许可证。相关许可证和署名文件见 `source/` 及成品 JAR 的 `META-INF/`。
