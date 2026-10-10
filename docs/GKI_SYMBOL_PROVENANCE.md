# ANB GKI 符号定义及 Android Clang12 控制实验 — 已验证结果

**状态：成功，独立 GitHub Actions #38076177418，证据已永久归档。**

固定 Google 公共源码 `android12-5.10-2025-05_r1` / `b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e`。设备原始配置 SHA-256 `76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3`。

## A. 全树 Kconfig 符号定义清点

扫描 **1,580 个 Kconfig 文件**。对旧宿主 Clang14 比较中“原本启用而求解后缺失”的 **208 项**，其中 **28 项在公共 Google 参考树中有定义**，**180 项全树未找到定义**。证据：[完整 JSON](../evidence/reports/anb-gki-symbol-definitions.json)；[逐项定义路径报告](GKI_SYMBOL_DEFINITIONS.md)。

- 静态扫描中“存在定义”不一定代表在本次 Kconfig 入口中可达。
- “全树未找到定义”也无法证明荣耀独创，可能属于 Qualcomm/vendor 补丁或其它 Google 分支。
- 配置项名不是内核实现代码证据，不能直接推断可利用性。

## B. 实机相应编译器条件的第二次求解

从 Google 官方 `platform/prebuilts/clang/host/linux-x86` 下载 Android **Clang 12.0.5、r416183b**（预编译仓库提交 `2e72cd026daa0cac35883ba23c78ad1dcfa15cf4`）；GitHub Actions 已核对 `clang --version` / `ld.lld --version`，在 `ARCH=arm64 LLVM=1 LLVM_IAS=1` 下运行 `olddefconfig`。

| 项目 | 旧宿主 Ubuntu Clang14 | Android Clang12.0.5 |
|---|---:|---:|
| 原始设备配置项 | 5,672 | 5,672 |
| GKI 参考树求解后 | 5,351 | **5,366** |
| 共享配置取值变化 | 5 | **0** |
| 原始项求解后缺失 | 329 | **311** |
| 原已启用而缺失 | 208 | **193** |
| 新增项 | 8 | **5** |

旧 208 项中有 **16 项**在 Clang12 求解后恢复（含 `CONFIG_CFI_CLANG`、`CONFIG_LTO_CLANG_FULL`、`CONFIG_AS_IS_LLVM`、`CONFIG_RELR` 等）；新实验相对旧实验又有 **1 个**原已启用符号新缺失：`CONFIG_AS_HAS_CFI_NEGATE_RA_STATE`。总量净减少 15。原因分类需继续进行更严格源码和 Kconfig 依赖核对，不能把全部变化简单归因于 Clang 版本单一因素（LLVM 汇编器设定同时变化）。

完整机器报告：[Clang12 JSON](../evidence/reports/anb-gki-clang12-comparison.json)；[汇总](GKI_CLANG12_COMPARISON.md)；[Android Clang 版本文本](../evidence/reports/anb-clang12-version.txt)、[LLD 版本](../evidence/reports/anb-lld12-version.txt)、[工具链来源提交](../evidence/reports/anb-toolchain-source-commit.txt)。

## C. 实验溯源与错误历史

- [Actions #38076177418：两实验全部成功](https://github.com/Chuniex-0631/honor-anb-an00-research/actions/runs/38076177418)，Artifact `anb-gki-symbol-clang12-38076177418`（ID 11678900920），包含原始/求解后配置、编译器日志、逐项差异与 JSON，下载文件的服务器端登记摘要 `sha256:8f01c651d93415e38af44f490e765477d0c8eebafcc98c9c7320d8da8237ad12`。Artifacts 可能过期，永久摘要数据已进入 Git。
- [归档工作流 #38076601768](https://github.com/Chuniex-0631/honor-anb-an00-research/actions/runs/38076601768) **success**：固定源版本、工具链身份、原始 Kconfig 哈希以及新统计全部验证后，再写入上述报告。
- 早期失败 [#38076018784](https://github.com/Chuniex-0631/honor-anb-an00-research/actions/runs/38076018784) 和 [#38076037276](https://github.com/Chuniex-0631/honor-anb-an00-research/actions/runs/38076037276)：Kconfig 解析器对非标准符号字符范围处理与预期不一致，初次工作流亦带入 Artifact 文件名反斜杠；后续修正、再跑成功。其失败**不是设备实验失败或 GKI 不兼容**。

## D. 限制与下一步

两轮均比较 **Google 公共参考树**，不是 HONOR ANB-AN00 9.0.0.230 的原厂源码。后续按 193 项缺失激活列表结合 180 项公共树无定义列表进行优先级交叉比对，并继续争取实际 OEM 发布源码/补丁。必须实证核验 CVE 代码，而不能由宏名推断漏洞。未获取 Root、Bootloader 解锁或适配刷机文件；无实体机操作。
