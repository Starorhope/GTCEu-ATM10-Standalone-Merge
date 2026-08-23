---
navigation:
  parent: index.md
  title: 使用技巧
  icon: minecraft:writable_book
  position: 20
---

# 使用技巧

## 在多方块机器中合成同时包含物品和流体的配方

可以使用子网更高效地处理这类合成配方。  
本模组也会修改子网内机器的电路编号。  
请参考下图。

<GameScene zoom="4" background="transparent" interactive={true}>
<ImportStructure src="../structure/provider_interface_storage.snbt" />

<BoxAnnotation color="#dddddd" min="2.7 0 0" max="3 1 1">
        接口
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 0 0" max="1.3 2 1">
        存储总线
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="0 0 0" max="1 2 1">
        总线与仓口
  </BoxAnnotation>

<IsometricCamera yaw="200" pitch="30" />
</GameScene>

将所有存储总线的“报告无法访问的物品”设为“是”。  
若保持禁用，阻挡模式可能无法正常工作。  
![](../pic/storage_bus_setting.png)
