# Build Notes

The published JAR was assembled from the exact ATM10-compatible base artifact and the two source projects in this repository. The merge is intentionally a single logical `gtceu` container; the embedded feature modules do not add standalone NeoForge mod entries.

Expected inputs:

```text
base GTCEu  : gtceu-1.21.1-8.0.1-atm10.5.jar
GTNA         : gtna-0.4.0.jar
PCCard       : pccard-1.21.1-1.3.1+neoforge.1.21.1.jar
bridge       : gtceu-embedded-addons-bridge.jar
LDLib 1.x    : ldlib-neoforge-1.21.1-1.0.41.jar
```

Example:

```powershell
python tools/merge_into_gt.py `
  --base path/to/gtceu-1.21.1-8.0.1-atm10.5.jar `
  --gtna path/to/gtna-0.4.0.jar `
  --pccard path/to/pccard-1.21.1-1.3.1+neoforge.1.21.1.jar `
  --bridge path/to/gtceu-embedded-addons-bridge.jar `
  --ldlib path/to/ldlib-neoforge-1.21.1-1.0.41.jar `
  --output release/gtceu-standalone.jar `
  --manifest release/merge-report.json
```

Do not commit ATM10 instance directories, Gradle caches, local dependency JARs, access tokens, or game logs. The release JAR is the tested deliverable; source builds still require obtaining the upstream dependencies under their respective licenses.
