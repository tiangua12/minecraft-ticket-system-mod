# 铁路票务系统 Web API 文档

本文档描述 Minecraft Forge 模组 "火车票" 提供的 Web API 接口。启动 Web 服务器后，可通过 `http://localhost:端口号/api` 访问。

## 快速开始

1. 在游戏中执行命令 `/webserver start` 启动 Web 服务器
2. 默认端口为 `23333`（可在配置中修改）
3. 访问 `http://localhost:23333/` 打开 Web 控制台
4. API 根地址为 `http://localhost:23333/api`

## 基础信息

### 健康检查
```http
GET /api/health
```
返回服务器运行状态。

**响应示例：**
```json
{
  "ok": true,
  "service": "ticketsystem",
  "timestamp": 1734048000000,
  "port": 23333,
  "running": true
}
```

### API 根信息
```http
GET /api
```
返回可用的 API 端点列表。

**响应示例：**
```json
{
  "ok": true,
  "service": "FTC Ticketing System API",
  "version": "1.0",
  "endpoints": [
    "/api/stations",
    "/api/lines",
    "/api/fares",
    "/api/config",
    "/api/export",
    "/api/health"
  ]
}
```

## 车站管理

### 获取所有车站
```http
GET /api/stations
```
返回所有车站的列表。

**响应示例：**
```json
[
  {
    "code": "01-01",
    "name": "北京站",
    "en_name": "Beijing Station",
    "x": 100,
    "y": 64,
    "z": 200
  }
]
```

### 添加车站
```http
POST /api/stations
Content-Type: application/json

{
  "code": "01-02",
  "name": "上海站",
  "en_name": "Shanghai Station",
  "x": 200,
  "y": 64,
  "z": 300
}
```

**参数说明：**
- `code` (必需)：车站编码，格式 "线路号-站序号"，如 "01-01"
- `name` (必需)：车站中文名称
- `en_name` (可选)：车站英文名称
- `x`, `y`, `z` (可选)：车站坐标，用于地图显示

### 更新车站
```http
PUT /api/stations/{code}
Content-Type: application/json

{
  "name": "上海站（新）",
  "en_name": "Shanghai New Station"
}
```
更新指定编码的车站信息。

### 删除车站
```http
DELETE /api/stations/{code}
```
删除指定编码的车站。

## 线路管理

### 获取所有线路
```http
GET /api/lines
```
返回所有线路的列表。

**响应示例：**
```json
[
  {
    "id": "L1",
    "name": "1号线",
    "en_name": "Line 1",
    "color": "#3366CC",
    "stations": ["01-01", "01-02", "01-03"]
  }
]
```

### 获取单个线路
```http
GET /api/lines/{id}
```
返回指定 ID 的线路详情。

### 添加线路
```http
POST /api/lines
Content-Type: application/json

{
  "id": "L2",
  "name": "2号线",
  "color": "#FF6600",
  "stations": ["02-01", "02-02"]
}
```

**参数说明：**
- `id` (必需)：线路唯一标识
- `name` (必需)：线路名称
- `color` (可选)：线路颜色（HEX 或颜色名），默认 "#3366CC"
- `stations` (可选)：车站编码列表，按顺序排列

### 更新线路
```http
PUT /api/lines/{id}
Content-Type: application/json

{
  "name": "2号线（延长）",
  "stations": ["02-01", "02-02", "02-03"]
}
```

### 删除线路
```http
DELETE /api/lines/{id}
```
删除指定 ID 的线路。

## 票价管理

### 获取所有票价
```http
GET /api/fares
```
返回所有票价段列表。

**响应示例：**
```json
[
  {
    "from": "01-01",
    "to": "01-02",
    "cost": 5,
    "cost_regular": 5,
    "cost_express": 10
  }
]
```

### 添加票价（单个）
```http
POST /api/fares
Content-Type: application/json

{
  "from": "01-01",
  "to": "01-02",
  "cost_regular": 5,
  "cost_express": 10
}
```

**参数说明：**
- `from` (必需)：出发站编码
- `to` (必需)：到达站编码
- `cost_regular` (必需)：普通票价
- `cost_express` (可选)：特急票价（默认与普通票价相同）

### 批量添加票价
```http
POST /api/fares/bulk
Content-Type: application/json

{
  "segments": [
    {"from": "01-01", "to": "01-02"},
    {"from": "01-02", "to": "01-03"}
  ],
  "cost_regular": 5,
  "cost_express": 10
}
```
或
```json
{
  "fares": [
    {"from": "01-01", "to": "01-02", "cost_regular": 5, "cost_express": 10},
    {"from": "01-02", "to": "01-03", "cost_regular": 5, "cost_express": 10}
  ]
}
```

### 更新票价
```http
PUT /api/fares
Content-Type: application/json

{
  "from": "01-01",
  "to": "01-02",
  "cost_regular": 6,
  "cost_express": 12
}
```

### 删除票价
```http
DELETE /api/fares
Content-Type: application/json

{
  "from": "01-01",
  "to": "01-02"
}
```
或通过路径参数：
```http
DELETE /api/fares/{from}/{to}
```

## 配置管理

### 获取配置
```http
GET /api/config
```
返回当前系统配置。

**响应示例：**
```json
{
  "api_base": "http://127.0.0.1:23333/api",
  "current_station": {
    "name": "Station1",
    "code": "01-01"
  },
  "transfers": [],
  "promotion": {
    "name": "",
    "discount": 1.0
  }
}
```

### 更新 API 地址
```http
PUT /api/config/api_base
Content-Type: application/json

{
  "api_base": "http://192.168.1.100:23333/api"
}
```

### 更新换乘映射
```http
PUT /api/config/transfers
Content-Type: application/json

{
  "transfers": [
    ["01-03", "02-04"],
    ["01-05", "02-07"]
  ]
}
```

### 更新优惠设置
```http
PUT /api/config/promotion
Content-Type: application/json

{
  "name": "春节优惠",
  "discount": 0.8
}
```

### 批量更新配置
```http
PUT /api/config
Content-Type: application/json

{
  "api_base": "http://127.0.0.1:23333/api",
  "promotion": {
    "name": "夏季促销",
    "discount": 0.9
  }
}
```

## 数据导出

### 导出所有数据
```http
GET /api/export
```
导出车站、线路、票价和配置的完整数据快照。

**响应示例：**
```json
{
  "stations": [...],
  "lines": [...],
  "fares": [...],
  "config": {...}
}
```


## 操作日志

### 记录操作日志
```http
POST /api/log
Content-Type: application/json

{
  "type": "login",
  "detail": {"user": "admin"}
}
```

### 获取操作日志
```http
GET /api/logs?max=100
```
获取最近的操作日志。

## 统计信息

### 票务统计（按天）
```http
GET /api/stats/ticket/byDay
```

### 票务统计（按小时）
```http
GET /api/stats/ticket/byHour
```

### 票务总计
```http
GET /api/stats/ticket/total
```

### 闸机统计
```http
GET /api/stats/gate/byDay
GET /api/stats/gate/byHour
GET /api/stats/gate/total
```

## 错误处理

所有 API 在出错时返回以下格式：

```json
{
  "ok": false,
  "error": "错误描述信息"
}
```

**常见 HTTP 状态码：**
- `200`：成功
- `201`：创建成功
- `400`：请求参数错误
- `404`：资源未找到
- `405`：方法不允许
- `500`：服务器内部错误

## 跨域支持

所有 API 端点都设置了 `Access-Control-Allow-Origin: *` 头部，支持跨域请求。

## 注意事项

1. 所有数据存储在 `mods/ticketsystem/` 目录下的 JSON 文件中
2. 车站编码格式必须为 "线路号-站序号"（如 "01-01"）
3. 票价以铜币（copper）为单位
4. 折扣系数范围为 0.0-1.0（0.8 表示 8 折）

## 更新日志

- v1.0 (2024-10-26): 初始版本，支持车站、线路、票价基础管理
- v1.1 (2024-12-13): 增加车票日志、操作日志、统计 API 说明

---

*本文档适用于火车票模组 v1.0+。API 可能随版本更新而变化。*