# ViScriptShop
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/zhenshiz/ViScriptShop)

服务于ViScriptNpc的商店模组，采用更加现代化的购物车商店系统

模组前置：ldlib2

模组联动：KubeJS，精妙背包，Backpacked，超越维度，FTB Library，JEI，LightmansCurrency，汇流来世，Magic Coins，jech

安装 Backpacked 3.0.5 或更新的兼容版本后，购买按钮旁的物品输出按钮可切换至 Backpacked，图标使用背包物品，选择会随玩家数据保存。物品输出至已装备背包的已解锁格子；未装备背包时拒绝交易且不扣款，容量不足的余量由 VSL 掉落在玩家脚下。Backpacked 及其前置 Framework 需要另行安装。

模组wiki：https://doc.mafuyu.moe/wiki/ViScriptShop

购买协议仅提交商店相对路径、分类 ID、商品 ID、购买数量及物品输出位置。服务端校验整份清单，并从自己的商店配置计算成本、收益、促销和指令；无效商品、重复条目或非正数量会拒绝整笔交易。商店文件上传需要服务端 4 级权限。购买和材料计数 RPC 使用新的 `v2` 标识，升级时需要同步更新客户端与服务端。

构建会嵌入当前工程的 ViScriptLib，确保通用编辑器上传权限检查一并生效。部署时应同时替换单独安装的旧版 ViScriptLib。

商店按窗口大小独立缩放，原版 GUI 缩放不影响商店组件大小。深色玻璃主题的高度保持屏幕的 91%，灰猫工坊按原有美术比例确定基准高度；客户端配置 `config/viscript_shop_client.toml` 中的 `[client].shopContentScale` 控制内部内容倍率，默认 `1.0`，范围 `0.5`～`1.5`。减小可容纳更多内容，增大可放大文字和物品，重新打开商店后生效。内容倍率适用于两个主题。

列表商品卡片的宽高随内容倍率一起缩放，中间的搜索栏和商品列表随卡片收窄。列表高度和左右两栏宽度保持不变，省出的宽度直接从商店外框扣除，整体保持居中。缩小内容倍率可同时显示更多商品行；序号与成本之间保持固定间距，产出右侧为赠品保留独立位置。

顶部分类标题、搜索框、图标、头像和商店标题保持默认倍率下的显示大小，顶栏高度不随内容倍率变化；空间不足时搜索框仅缩短宽度。新的默认 `1.0` 对应原来的 `1.25`，较大倍率会按窗口可用宽度自动限制，避免三栏超出屏幕。

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
| `./gradlew :ViScriptShop:runClient -PldTest=shop_independent_scale -PldTestWindow=960x540` | 验证不同原版 GUI 缩放、窗口比例和商店内容倍率下的布局及点击命中，输出实机截图 |
| `./gradlew :ViScriptShop:runClient -PldTest=vss_backpacked_output -PbackpackedTest -PldTestWindow=1280x720` | 临时加载 Backpacked 和 Framework，验证真实输出按钮、购买发货、选择同步和未装备背包的扣款保护 |
| `./gradlew buildAll` | 构建所有子项目，产物在主工程根目录 `build/libs` |
| `./gradlew cleanLibs` | 清理所有子项目的构建产物 |

对本仓库的修改需要以子模块的形式提交并推送到本仓库；随后在主工程中再提交一次指向新 commit 的子模块引用。
