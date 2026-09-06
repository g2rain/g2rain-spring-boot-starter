# 发布说明

## 快照发布

`develop` 分支推送或手动触发 `snapshot.yml`。工作流仅在项目版本以 `-SNAPSHOT` 结尾时执行部署，并使用 `central-portal-snapshots` 凭据；部署命令跳过测试，因此测试必须由前置验证保证。

## 正式发布

推送 `v*.*.*` 标签或手动触发 `release.yml`，使用 JDK 25 执行：

```shell
mvn -B -P release clean deploy
```

release Profile 会附加源码与 Javadoc、执行 GPG 签名，并由 Central Publishing 插件自动发布且等待发布完成。

## 发布检查

- 根 POM、README、CHANGELOG 和标签版本一致。
- 12 个 Reactor 项目均在预期模块列表内，`mvn clean verify` 通过。
- 内部 G2rain 依赖版本已存在于目标仓库。
- Central Portal Token、GPG 私钥和口令仅存于受保护的 CI Secret。
- 检查每个 JAR/POM 的坐标、源码、Javadoc 与签名。

正式发布不可作为普通验证命令执行。
