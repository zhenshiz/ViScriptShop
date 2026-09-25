# ViScriptShop
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/zhenshiz/ViScriptShop)

服务于ViScriptNpc的商店模组，采用更加现代化的购物车商店系统

模组前置：ldlib2

模组联动：KubeJS，精妙背包，超越维度，FTB Library，JEI，LightmansCurrency，汇流来世，Magic Coins，jech

模组wiki：https://doc.mafuyu.moe/wiki/ViScriptShop

## 开发与构建

ViScriptShop 现在作为 [ViScriptLib](https://github.com/zhenshiz/ViScriptLib) 多项目工程的一个子模块开发。**本仓库的代码无法单独构建和启动**：构建脚本依赖主工程统一提供的插件、依赖版本与运行配置，编译期也依赖同工程中的 ViScriptLib 子项目。

请通过主工程获取完整代码：

```bash
# 克隆主工程并同时拉取全部子模块
git clone --recursive https://github.com/zhenshiz/ViScriptLib.git
cd ViScriptLib

# 已克隆过主工程时，可手动拉取/更新子模块
git submodule update --init --recursive
```

常用命令（均在主工程根目录执行）：

| 命令 | 作用 |
| --- | --- |
| `./gradlew :ViScriptShop:build` | 单独构建本模组 |
| `./gradlew :ViScriptShop:runClient` | 启动客户端调试本模组 |
| `./gradlew buildAll` | 构建所有子项目，产物在主工程根目录 `build/libs` |
| `./gradlew cleanLibs` | 清理所有子项目的构建产物 |

对本仓库的修改需要以子模块的形式提交并推送到本仓库；随后在主工程中再提交一次指向新 commit 的子模块引用。
