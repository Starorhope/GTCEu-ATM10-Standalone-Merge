# GTCEu ATM10 Standalone Merge

这是 GregTech Modern（GTM，项目名称也常写作 **GregTech CEu: Modern / GTCEu Modern**）面向 **All the Mods 10 8.0 Migrated**、Minecraft 1.21.1 和 NeoForge 21.1.1 的**非官方移植与维护构建**。

本发布包把 GTNA（GregTech: Nexus Addon）和 Programmed Circuit Card 的代码、资源、配方与兼容层嵌入 GTCEu 容器中。

## 下载

将 `release/gtceu-1.21.1-8.0.1-atm10.5-with-gtna-pccard-recipefix-v6.jar` 放入 NeoForge 1.21.1 的 `mods` 目录。

SHA-256:

`BF2888E67BED0E447482E8EEE8E8AAC2363AE795C0976E8997CAF05005BCF136`

## 验证结果

- NeoForge: `21.1.248`
- Minecraft: `1.21.1`
- GTCEu 容器版本: `8.0.1-atm10.5`
`

完整审计报告位于 `release/`。报告中的本机路径只用于记录生成环境，不是运行时依赖。

## 构建

需要 JDK 21、Gradle 8.14、NeoForge 1.21.1 依赖，以及 ATM10 对应的 GTCEu、LDLib、AE2、GuideME、AdvancedAE、JEI 等依赖。源代码位于 `source/gtna`、`source/pccard` 和 `source/bridge`；合并脚本位于 `tools/`。基础 GTCEu 源码不随本仓库完整复制，构建者应按其上游许可证取得与 `release/merge-report-recipefix-v6.json` 中哈希相符的基础 JAR。

构建两个源项目后，使用 `tools/merge_into_gt.py` 将它们和桥接 Mixin 嵌入基础 GTCEu JAR，再执行 `tools/audit_release.py` 与 `tools/audit_localization.py`。脚本会校验 ZIP CRC、重复路径、Mixin 类、NeoForge 依赖、语言键和嵌入模块，遇到未声明冲突会直接失败。

## 许可与来源

版权和许可证按组件分别适用，不能把整个合并 JAR 视为一个新的、单一的许可证作品。请先阅读 [`LICENSE-NOTICE.md`](LICENSE-NOTICE.md) 和 [`NOTICE.md`](NOTICE.md)，并在再分发时保留 `source/` 中的上游许可证/署名文件以及成品 JAR 的 `META-INF/` 通知。

主要上游：

- GTM / GTCEu Modern：<https://github.com/GregTechCEu/GregTech-Modern>
- GregTech Nexus Addon（GTNA）：<https://github.com/Raishxn/GregTech-Nexus-Addon>
- Programmed Circuit Card：<https://github.com/yuuki1293/ProgrammedCircuitCard>
- All the Mods 10（兼容目标）：<https://www.curseforge.com/minecraft/modpacks/all-the-mods-10>

“GregTech”“GTCEu”“GTM”“All the Mods”“Minecraft”等名称和标志属于各自权利人。本仓库仅以兼容性和来源说明为目的使用这些名称，不主张商标权，也不暗示任何赞助、授权或背书。许可证整理仅供项目使用，不构成法律意见；有疑问时请向相应权利人核实。完整边界和组件清单见 [`LICENSE-NOTICE.md`](LICENSE-NOTICE.md)。
