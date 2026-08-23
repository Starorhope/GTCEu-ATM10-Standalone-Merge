---
navigation:
  parent: index.md
  title: 编程电路卡
  icon: pccard:card_programmed_circuit
  position: 10
categories:
  - tools
item_ids:
  - pccard:card_programmed_circuit
---

# 编程电路卡

安装此卡的样板供应器会直接修改 GregTech 机器的电路配置，而不会把编程电路作为物品推入机器。

## 配方

<RecipeFor id="pccard:card_programmed_circuit" />

## 样板注册

请在样板中加入编程电路。即使网络中没有编程电路，该样板也能正常工作。  
每次手动放入电路很不方便，因此从 JEI 或 EMI 注册配方时，本模组会自动把编程电路加入样板。  
若编程电路没有被加入，或希望禁用此功能，请检查配置中的 `jei_integration` 设置。  
![样板注册](../pic/encode_pattern.png)

## 样板供应器

将编程电路卡放入样板供应器的升级槽。  
**不要忘记将样板供应器设为阻挡模式。**  
![样板供应器](../pic/pattern_provider.png)

### 注意

> - 不含编程电路的样板会被视为电路编号 0。
> - 若一台机器旁连接了多个样板供应器，阻挡模式可能无法正常工作。

## 合成请求

你可以像订购普通物品一样订购它。编程电路不会出现在合成计划中。  
若它仍然出现，请确认样板供应器中已安装编程电路卡。
