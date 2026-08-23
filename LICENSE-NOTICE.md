# License and Copyright Notice

## Project status

This repository distributes an **unofficial, community-maintained port and integration build** of GregTech Modern (GTM, also referred to as GregTech CEu: Modern or GTCEu Modern) for the All the Mods 10 8.0 Migrated environment and Minecraft 1.21.1. It is built against NeoForge 21.1.248 and declares NeoForge 21.1.240 as the minimum runtime version. The word “port” describes the version adaptation and packaging work in this repository; it does not claim that the upstream project authorized, reviewed, or endorsed this build.

This project is not the official GTM/GTCEu or GregTechCEu project, is not an official All the Mods release, and is not endorsed, sponsored, or supported by:

- the GregTechCEu / GTM upstream maintainers;
- the All The Mods team;
- Raishxn, the GTNA author; or
- yuuki1293, the Programmed Circuit Card author.

The names and marks GregTech, GTCEu, GTM, All the Mods, Minecraft, NeoForge, and the names of other dependencies remain with their respective owners. They are used here only to identify compatibility, provenance, and required software. No trademark license or ownership is claimed, and nothing in this repository should be presented as an official release.

## Component licensing

The merged JAR is a packaging of components with separate copyright and license terms. Combining files into the `gtceu` container does **not** relicense, waive, or transfer any upstream rights. The following list is a guide, not a replacement for the license and notice files shipped with each component:

| Component | Copyright / provenance | License and notice |
| --- | --- | --- |
| GTM / GTCEu base artifact | GregTechCEu upstream and contributors; upstream baseline [`8201bf3847792e40b0dd75a6952e76c4c72e3cbf`](https://github.com/GregTechCEu/GregTech-Modern/commit/8201bf3847792e40b0dd75a6952e76c4c72e3cbf); custom base SHA-256 `D922311A96FA607BC15FD5E8056A11D84440FD72897099BCB05690E36185DD19` | The custom base is not identical to the upstream commit and its complete corresponding source is not currently present in this repository. Its metadata declares `LGPL-3.0-only AND GPL-2.0-or-later AND LGPL-2.1-or-later AND MPL-2.0 AND MIT AND Apache-2.0 AND OFL-1.1` across its code and bundled materials. Preserve the notices in the JAR's `META-INF/`. |
| GregTech Nexus Addon (GTNA) | Upstream project by Raishxn and its contributors | GNU LGPL v3.0 as distributed in [`source/gtna/LICENSE.MD`](source/gtna/LICENSE.MD). Individual files that carry their own SPDX or copyright headers retain those terms as well. |
| Programmed Circuit Card | Copyright (c) 2024 yuuki1293 and contributors | MIT License in [`source/pccard/LICENSE.txt`](source/pccard/LICENSE.txt); retain [`source/pccard/CREDITS.txt`](source/pccard/CREDITS.txt). |
| Embedded-addons bridge | Contributors to this repository | GNU LGPL-3.0-or-later; see [`source/bridge/LICENSE.md`](source/bridge/LICENSE.md). This license covers only the bridge files authored for this repository, not GTCEu, GTNA, PCCard, or other embedded code. |
| LDLib and other bundled or runtime dependencies | Their respective copyright holders | Follow every license and attribution notice in the final JAR's `META-INF/`, including `THIRD_PARTY_NOTICES.md` and nested-library notices. |

The GTNA and PCCard source trees are included to make the corresponding modified portions auditable. The complete GTCEu source tree and local dependency artifacts are not copied into this repository; obtain them from their upstream projects under the applicable licenses when reproducing a build.

## Publication gate

The `v7` binary is a local release candidate and must not be published until the complete corresponding source for the modified `8.0.1-atm10.5` GTCEu base is made available from the same distribution location. A generic upstream link is insufficient because the base manifest records a dirty custom fork build. Public redistribution also requires confirmation that every ATM9/ATM10-derived quest, texture, model, and script in the base artifact may be redistributed under its applicable terms. This gate does not alter or excuse the obligations attached to any already distributed binary.

## Redistribution requirements

When redistributing this source or binary:

1. Keep the upstream license texts, copyright statements, credits, and `META-INF/` notices intact.
2. Identify your changes and do not imply that they were authored, tested, or approved by an upstream project or by All the Mods.
3. Observe the applicable requirements for modified LGPL/GPL/MPL-covered code, including source and relinking obligations where they apply. This repository provides the included GTNA source tree and the bridge source under `src/main`, but does not make a separate legal determination for a downstream distribution.
4. Do not remove or alter trademark notices in a way that suggests affiliation, sponsorship, or endorsement.

The merge changes the distribution layout (GTNA and PCCard are embedded under one logical `gtceu` mod container) and includes compatibility, recipe, and localization changes. It does not change the copyright owner or license of the material being embedded.

## Disclaimer

The software and documentation are provided “as is”, without warranty to the extent permitted by the applicable licenses. Compatibility is tested only against the versions listed in the README and may change with other modpack or dependency versions. This file records provenance and licensing information for practical redistribution; it is not legal advice. If a notice appears incomplete or a rights holder requests a correction, contact the repository maintainer and consult the relevant upstream project.

## Upstream links

- GTM / GTCEu Modern: <https://github.com/GregTechCEu/GregTech-Modern>
- GregTech Nexus Addon: <https://github.com/Raishxn/GregTech-Nexus-Addon>
- Programmed Circuit Card: <https://github.com/yuuki1293/ProgrammedCircuitCard>
- All the Mods 10: <https://www.curseforge.com/minecraft/modpacks/all-the-mods-10>
- GNU LGPL v3.0 text: <https://www.gnu.org/licenses/lgpl-3.0.html>
- MIT License text: <https://opensource.org/license/mit>
