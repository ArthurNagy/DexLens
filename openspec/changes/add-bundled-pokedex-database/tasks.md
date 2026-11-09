## 1. Module and schema

- [x] 1.1 `:core:database`: create the module, entities, DAOs and `DexDatabase` with the exported schema
- [x] 1.2 `:core:database`: wire `createFromAsset` and the bundled driver in `DatabaseModule`
- [x] 1.3 `:core:data`: depend on `:core:database`; update `CLAUDE.md` modules

## 2. Build script

- [x] 2.1 `tools/build_database.py`: pinned shallow clone, schema-driven table creation
- [x] 2.2 `tools/build_database.py`: import species, names, varieties, forms, abilities, egg groups, evolutions, types
- [x] 2.3 `tools/build_database.py`: build the FTS5 index and validate counts and tricky names
- [x] 2.4 Generate and commit `core/database/src/main/assets/databases/dexlens.db`

## 3. Tests

- [x] 3.1 `:core:database`: run tests on the host-JVM bundled driver
- [x] 3.2 `:core:database`: test every scenario against the real asset, tagged `// Scenario:`
- [x] 3.3 `:core:database`: instrumented test on the device for the prebuilt file with FTS5
- [x] 3.4 Record ADR 0010 and run the local gate: `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true`
