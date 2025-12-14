# 火车票模组 - Web服务器管理指令

## 概述
本模组添加了用于手动管理Web服务器的指令，方便调试和控制Web服务器的启动、停止和状态检查。

## 指令结构
所有指令都以 `/ticketsystem` 开头，需要管理员权限（权限等级4）。

## 新增Web服务器管理指令

### 1. 启动Web服务器
```bash
/ticketsystem startwebserver
```
**功能**：
- 手动启动Web服务器
- 如果Web服务器已在运行，显示当前运行状态
- 启动成功时显示端口号

**响应示例**：
- 成功：`Web服务器启动成功，端口: 23333`
- 已运行：`Web服务器已在运行，端口: 23333`
- 失败：`Web服务器启动失败，请查看日志获取详细信息`

### 2. 停止Web服务器
```bash
/ticketsystem stopwebserver
```
**功能**：
- 手动停止Web服务器
- 如果Web服务器未在运行，显示相应提示

**响应示例**：
- 成功：`Web服务器已停止，端口: 23333`
- 未运行：`Web服务器未在运行`

### 3. 查看Web服务器状态
```bash
/ticketsystem webserverstatus
```
**功能**：
- 查看Web服务器当前运行状态
- 如果正在运行，显示端口号和访问地址

**响应示例**：
- 运行中：`Web服务器状态: 运行中，端口: 23333，访问地址: http://localhost:23333/`
- 已停止：`Web服务器状态: 已停止`

## 使用场景

### 场景1：手动启动Web服务器
当自动启动失败或需要手动控制时：
```bash
/ticketsystem startwebserver
```

### 场景2：重启Web服务器
```bash
/ticketsystem stopwebserver
/ticketsystem startwebserver
```

### 场景3：调试Web服务器问题
```bash
/ticketsystem webserverstatus
```

## 配置说明
Web服务器的配置在 `ticketsystem-common.toml` 文件中：
```toml
["Web服务器设置"]
    # Web服务器端口 (默认: 23333)
    web_server_port = 23333
    # 是否启用Web服务器 (默认: true)
    web_server_enabled = true
```

## 常见问题

### Q1: 启动Web服务器时提示"Permission denied"
**原因**：端口号低于1024需要管理员权限
**解决**：修改配置使用高于1024的端口，如8080、8888等

### Q2: Web服务器启动但无法访问
**检查步骤**：
1. 使用 `/ticketsystem webserverstatus` 确认服务器状态
2. 检查防火墙是否允许该端口
3. 确认使用正确的访问地址：`http://localhost:端口号/`

### Q3: 命令执行没有响应
**原因**：可能权限不足或命令注册失败
**解决**：
1. 确认拥有权限等级4（OP权限）
2. 检查游戏日志是否有错误信息
3. 确认模组已正确加载

## 日志信息
Web服务器相关操作会在服务器日志中记录：
- 启动成功：`Web服务器通过命令手动启动成功，端口: 23333`
- 启动失败：`Web服务器通过命令手动启动失败`
- 停止成功：`Web服务器通过命令手动停止，端口: 23333`

## 自动化启动
除了手动启动，Web服务器也可以在模组初始化时自动启动（需在配置中启用）：
```java
// 自动启动逻辑（TicketSystemMod.java）
if (TicketSystemConfig.isWebServerEnabled()) {
    WebServer.start();
}
```

## 技术支持
如需进一步帮助，请检查：
1. 服务器日志文件：`logs/latest.log`
2. 配置文件：`config/ticketsystem-common.toml`
3. 端口占用情况：`netstat -tlnp | grep 端口号`

---

**注**：Web服务器功能依赖于JDK内置的 `com.sun.net.httpserver.HttpServer`，请确保运行环境支持。