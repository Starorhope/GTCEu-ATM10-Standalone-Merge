# GTCEu ATM10 Standalone Merge

这是 GregTech Modern（GTM，也称 **GregTech CEu: Modern / GTCEu Modern**）面向 **All the Mods 10 8.0 Migrated** 和 Minecraft 1.21.1 的非官方移植与维护构建。本仓库不代表 GTM、GTCEu、All the Mods 或其他上游团队。

成品把 GTNA（GregTech: Nexus Addon）和 Programmed Circuit Card 的代码、资源、配方与兼容层嵌入同一个 GTCEu 容器。运行时只安装合并后的 GTCEu JAR，不再同时安装两个附属模组的独立 JAR。

## 下载

本地候选版本：`v7`（尚未公开发布）

文件：`release/gtceu-1.21.1-8.0.1-atm10.5-with-gtna-pccard-recipefix-v7.jar`

SHA-256：

`92E80F14D3521E775E6688A276C33202047CF96D816C5F77862E525EE295BE66`

v7 仅将 NeoForge 最低运行要求从精确的 `21.1.248` 放宽为 `[21.1.240,)`；编译和此前实机验证仍使用 `21.1.248`。因此目前没有宣称低于 `21.1.240` 的兼容性，也没有把 v7 记作一次新的实机测试。

公开发布 v7 前必须同时提供自定义 GTCEu 基础构件的完整对应源码，并确认其中迁移自 ATM9/ATM10 的受限资源具有公开再分发授权。当前仓库只固定了上游基线与二进制哈希，尚未满足这两个发布条件，因此 v7 仅保留为本地测试候选。

## 构建

仓库根目录采用 NeoForge 1.21.1 ModDevGradle 模板式布局，包括 Gradle Wrapper、`settings.gradle`、`build.gradle`、`gradle.properties`、`src/main`、元数据模板和 GitHub Actions。根项目是装配与维护工程，不包含完整 GTCEu 上游源码。外部基础 JAR 的 SHA-256 为 `D922311A96FA607BC15FD5E8056A11D84440FD72897099BCB05690E36185DD19`，其上游基线固定为 GregTech Modern 提交 [`8201bf3`](https://github.com/GregTechCEu/GregTech-Modern/commit/8201bf3847792e40b0dd75a6952e76c4c72e3cbf)；该链接不是自定义 `8.0.1-atm10.5` 完整对应源码的替代品。

要求 JDK 21 和 Python 3。仅检查模板结构不需要本地模组依赖：

```powershell
.\gradlew.bat templateCheck
```

完整构建需要自行取得 ATM10 对应的基础 GTCEu JAR、LDLib 1.x JAR 和实例依赖：

```powershell
.\gradlew.bat buildMergedRelease `
  '-Pgtceu_base_jar=D:/path/gtceu-1.21.1-8.0.1-atm10.5.jar' `
  '-Pldlib_jar=D:/path/ldlib-neoforge-1.21.1-1.0.41.jar' `
  '-Patm10_mods_dir=D:/path/to/All the Mods 10 - 8.0 Migrated/mods'
```

该任务会构建 `source/gtna` 与 `source/pccard`，编译根目录 bridge，合并到外部 GTCEu 基础 JAR，再运行发布审计。默认附属产物位于 `source/*/build/libs/`；可用 `-Pgtna_jar=...` 和 `-Ppccard_jar=...` 覆盖。不要提交实例目录、依赖 JAR、Gradle 缓存、游戏日志或凭据。

GitHub Actions 执行不依赖本地游戏 JAR 的模板与三项目元数据检查。最终合并构建必须由构建者按各组件许可证自行提供依赖。

## 验证结果

- Minecraft：`1.21.1`
- NeoForge 编译目标：`21.1.248`
- NeoForge 最低运行要求：`21.1.240`
- GTCEu 容器版本：`8.0.1-atm10.5`
- v7 ZIP、Mixin、依赖与嵌入模块审计：通过
- 两次清理后的根目录完整构建哈希一致：通过
- v6 相同代码和资源的单包实机测试：通过；v7 未重新启动游戏

完整报告位于 `release/`。路径字段已使用仓库相对路径或外部文件占位符，输入和输出由 SHA-256 标识。

## 许可与来源

各组件继续适用各自许可证；合并分发不把它们重新许可为单一作品。再分发前请阅读 [`LICENSE-NOTICE.md`](LICENSE-NOTICE.md)、[`NOTICE.md`](NOTICE.md) 和 [`TEMPLATE_LICENSE.txt`](TEMPLATE_LICENSE.txt)，并保留源文件及成品 JAR 中的许可证和署名。

主要上游：

- NeoForge 模板：<https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle>
- GTM / GTCEu Modern：<https://github.com/GregTechCEu/GregTech-Modern>
- GregTech Nexus Addon：<https://github.com/Raishxn/GregTech-Nexus-Addon>
- Programmed Circuit Card：<https://github.com/yuuki1293/ProgrammedCircuitCard>
- All the Mods 10：<https://www.curseforge.com/minecraft/modpacks/all-the-mods-10>

“GregTech”“GTCEu”“GTM”“All the Mods”“Minecraft”等名称和标志属于各自权利人，仅用于说明兼容性和来源。本仓库不主张相关商标权，也不暗示任何赞助、授权或背书。许可证整理不构成法律意见。
