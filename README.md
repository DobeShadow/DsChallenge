# DsChallenge

每日随机挑战插件 —— 玩家每天通过跑马灯抽奖开启一个高难度主挑战与一个附属支线挑战，完成后获得丰厚奖励。

## 适用版本

- Minecraft 1.21.x（Purpur / Paper / Spigot）
- 可选依赖：Vault（用于经济奖励）

## 功能概览

- **每日抽取**：每个玩家每天可抽取一次今日挑战，抽取过程为跑马灯滚动动画
- **主挑战**：从击杀 / 采集 / 合成 / 钓鱼 / 交易 / 伤害 六类中随机抽取，数量为高难度档位
- **一命制**：主挑战进行中死亡即判定失败并锁定，当天无法再完成
- **支线挑战**：从生存 / 钓鱼 / 喂养 / 附魔 / 移动 / 小怪击杀 / 清理 七类中随机抽取
- **奖励发放**：主挑战完成时发放主奖励，并将支线奖励一并发放
- **全服广播**：主挑战完成时全服广播（可配置关闭）
- **每日重置**：默认每天 04:00 重置，重置前视为前一天
- **GUI 查看**：随时用 `/challenge` 查看今日挑战与进度

## 命令

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/challenge` | 打开今日挑战界面（未抽取时显示开启按钮，已抽取时显示详情） | `dschallenge.use`（默认所有人） |
| `/challenge refresh` | 重置自己的今日挑战，可重新抽取 | `dschallenge.admin`（默认 OP） |

别名：`/dc`、`/dailyc`

## 权限

| 权限节点 | 说明 | 默认 |
| --- | --- | --- |
| `dschallenge.use` | 使用 `/challenge` 打开界面 | 所有人 |
| `dschallenge.admin` | 使用 `/challenge refresh` | OP |

## 配置文件说明

配置文件位于 `plugins/DsChallenge/config.yml`。

### 基础设置

| 键 | 说明 |
| --- | --- |
| `config-version` | 配置结构版本，插件会校验，不匹配时提示 OP 更新配置 |
| `reset-time` | 每日重置时间，格式 `HH:mm`，默认 `04:00` |
| `notify-on-join` | 玩家进服时是否提示今日挑战 |
| `broadcast-on-complete` | 主挑战完成时是否全服广播 |

### 挑战池

- `main-pool`：主挑战类型池，可选 `KILL`、`MINE`、`CRAFT`、`FISH`、`TRADE`、`DAMAGE`
- `side-pool`：支线类型池，可选 `SURVIVE`、`SIDE_FISH`、`FEED`、`ENCHANT`、`MOVE`、`KILL_SMALL`、`CLEAN`

### 挑战参数

`challenges.<类型>` 下配置：

- `min` / `max`：随机数量范围（含两端）
- `targets`：可随机抽取的目标列表（怪物名、方块名、物品名等，使用 Bukkit 材质/实体名）

### 奖励配置

`rewards.main` 与 `rewards.side` 下配置：

```yaml
rewards:
  main:
    items:
      - material: DIAMOND
        amount: 16
      - material: GOLDEN_APPLE
        amount: 4
        name: '&6挑战者的金苹果'
    money: 5000
    commands:
      - 'give %player% netherite_ingot 2'
```

- `items`：物品奖励，`material` 为 Bukkit 材质名，`amount` 为数量，`name` 可选（支持 `&` 颜色代码）
- `money`：经济奖励金额，需要安装 Vault 且存在经济插件
- `commands`：命令奖励，`%player%` 会替换为玩家名，以控制台身份执行

### 消息文本

`messages` 下可自定义完成与失败提示，支持 `&` 颜色代码与 `%player%`、`%challenge%` 占位符。

## 数据存储

玩家数据保存在 `plugins/DsChallenge/playerdata.yml`，包含：

- 数据所属日期
- 今日主挑战与支线挑战内容
- 主/支线进度与状态

跨天时自动重置。

## 安装步骤

1. 将编译好的 `DsChallenge.jar` 放入服务器 `plugins/` 目录
2. 启动服务器，插件会自动生成 `config.yml`
3. 按需修改配置后重启或重载
4. 玩家输入 `/challenge` 开始使用

## 注意事项

- 主挑战为**一命制**，进行中死亡即失败锁定，请提醒玩家谨慎
- 生存类支线在死亡时进度会清零重新计时
- 移动类支线仅统计水平移动距离，跳跃与掉落不计入
- 未安装 Vault 时，经济奖励会被跳过并在控制台记录
