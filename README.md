# 餐饮外卖平台（sky-take-out）

一个面向餐饮外卖场景的 **Spring Boot 单体后端服务**，包含 **商家管理后台** 与 **用户端（小程序）** 两套接口，覆盖菜品管理、购物车、下单支付、订单状态流转、来单提醒、运营统计等完整业务闭环。

> 本项目为个人学习实践项目，用于展示 Java 后端开发能力。

---

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 语言 / 框架 | Java 8、Spring Boot 2.x、Spring MVC |
| 持久层 | MyBatis、Druid 连接池、PageHelper 分页 |
| 数据库 | MySQL 8 |
| 缓存 | Redis、Spring Cache（`@Cacheable` / `@CacheEvict`） |
| 定时任务 | Spring Task（`@Scheduled`） |
| 实时通信 | WebSocket（来单提醒、用户催单） |
| 鉴权 | JWT + ThreadLocal 登录态透传、拦截器校验 |
| 支付 | 微信 JSAPI 支付（预下单 + 支付回调） |
| 文件 / 报表 | 阿里云 OSS、Apache POI（Excel 导出） |
| 接口文档 | knife4j（Swagger） |
| 其他 | AOP + 自定义注解填充公共字段、Lombok、fastjson |

---

## 功能模块

### 商家管理端

| 模块 | 说明 |
| --- | --- |
| 员工管理 | 新增 / 分页查询 / 编辑 / 启用停用、登录鉴权 |
| 分类管理 | 菜品与套餐分类的增删改查 |
| 菜品管理 | 菜品新增（含口味）、图片上传 OSS、起售停售、批量删除、条件分页 |
| 套餐管理 | 套餐与菜品关联、起售停售、条件分页 |
| 订单管理 | 条件搜索、各状态订单统计、接单 / 拒单 / 派送 / 完成 |
| 店铺状态 | 营业 / 打烊，状态存 Redis 供两端读取 |
| 工作台 | 今日订单、营业额、有效订单、新增用户等看板数据 |
| 数据统计 | 近 30 日营业额 / 订单 / 用户 / 销量 Top10 图表数据 |
| 运营报表 | 基于 POI 模板导出近 30 日运营数据 Excel |

### 用户端（小程序）

| 模块 | 说明 |
| --- | --- |
| 微信登录 | 小程序 code 换 openid，JWT 签发令牌 |
| 菜品浏览 | 分类列表（Spring Cache 缓存）、菜品 / 套餐详情、口味选择 |
| 购物车 | 加购、减购、清空、列表查询 |
| 地址簿 | 收货地址增删改查 |
| 下单支付 | 提交订单（订单 + 明细同事务写入）、微信支付预下单、支付回调更新状态 |
| 订单查询 | 历史订单、订单详情、再来一单、催单 |
| 实时提醒 | WebSocket 推送来单提醒给商家端、催单消息 |

---

## 项目结构

```
sky-take-out
├── sky-common          公共模块：常量、异常、工具类、配置属性
├── sky-pojo            实体模块：entity / DTO / VO
├── sky-server          业务模块
│   └── src/main/java/com/sky
│       ├── controller  接口层（管理端 + 用户端）
│       ├── service     业务层
│       ├── mapper      持久层
│       ├── task        定时任务
│       ├── WebSocket   来单提醒 / 催单
│       ├── interceptor JWT 鉴权拦截器
│       ├── Aspect      公共字段自动填充
│       └── config      配置类
├── mp-weixin           小程序端代码
└── nginx-1.20.2        管理端前端静态资源与 Nginx
```

---

## 关键实现

**1. 缓存**
菜品分类等热点数据使用 Spring Cache + Redis 缓存，配合 `@CacheEvict` 在数据变更时清理，减少数据库压力。店铺营业状态直接存 Redis，读写都走缓存。

**2. 订单状态流转与超时处理**
用户下单时订单主表与订单明细在同一事务内写入（`@Transactional`），任一失败整体回滚。
`OrderTask` 通过 Spring Task 做两件事：
- 每分钟扫描「待付款且下单超过 15 分钟」的订单，自动取消；
- 每天凌晨 1 点扫描「派送中且超过 1 小时未完成」的订单，做异常处理。

**3. WebSocket 实时推送**
商家端连接 WebSocket 后，新订单进入时推送来单提醒；用户点击催单时推送催单消息，避免轮询。

**4. 鉴权与公共字段**
管理端与用户端各用一套 JWT 密钥与令牌请求头，拦截器解析后写入 ThreadLocal，业务层直接取当前登录用户。
公共字段（创建时间、更新时间、创建人、更新人）通过 AOP + 自定义注解自动填充，避免每个接口手动 set。

**5. 报表导出**
基于 Excel 模板用 Apache POI 填充近 30 日运营数据，保留模板样式。

---

## 本地运行

### 环境要求

- JDK 8
- MySQL 8
- Redis 6+
- Maven 3.6+

### 步骤

1. **建库导数据**

   ```bash
   mysql -uroot -p -e "CREATE DATABASE sky_take_out DEFAULT CHARSET utf8mb4;"
   mysql -uroot -p sky_take_out < sky-server/src/main/resources/sql/sky.sql
   ```

2. **配置本地参数**

   `application.yml` 中所有敏感项都通过 `${sky.*}` 占位符引用，真实值放在 `application-dev.yml`（**该文件已被 `.gitignore` 排除，需要自己创建**）：

   ```yaml
   sky:
     datasource:
       driver-class-name: com.mysql.cj.jdbc.Driver
       host: localhost
       port: 3306
       database: sky_take_out
       username: root
       password: 你的密码
     redis:
       host: localhost
       port: 6379
       password: 你的密码
       database: 10
     alioss:
       endpoint: 你的OSS_endpoint
       bucket-name: 你的bucket
     wechat:
       appid: 你的appid
       secret: 你的secret
     shop:
       address: 店铺地址
     baidu:
       ak: 你的百度地图ak
   ```

   > 没有微信支付 / OSS / 百度地图账号也能跑通除「支付」「图片上传」「距离计算」以外的全部功能。

3. **启动**

   ```bash
   mvn clean package -DskipTests
   java -jar sky-server/target/sky-server-1.0-SNAPSHOT.jar
   ```

   服务端口 `8080`，接口文档：`http://localhost:8080/doc.html`

4. **管理端前端**
   将 `nginx-1.20.2/html` 作为 Nginx 站点根目录，反向代理到 `8080`。小程序端用微信开发者工具打开 `mp-weixin`，把 `project.config.json` 里的 `appid` 换成自己的。

---

## Docker 部署

仓库自带 `Dockerfile`（多阶段构建）与 `docker-compose.yml`（MySQL 8 + Redis + 应用）：

```bash
docker compose up -d --build
```

启动后访问 `http://localhost:8080/doc.html`。

设计说明：

- **多阶段构建**：构建阶段用 Maven + JDK 8 编译打包，运行阶段只保留 JRE 8，最终镜像不含 Maven 与源码；
- **配置外置**：`application-dev.yml` 被 `.dockerignore` 排除，镜像里不含任何密钥。配置通过环境变量注入，由 Spring 的 relaxed binding 映射到 `${sky.*}` 占位符（如 `SKY_DATASOURCE_HOST` → `sky.datasource.host`）；
- **数据初始化**：首次启动自动执行 `sky-server/src/main/resources/sql/sky.sql`；
- **启动顺序**：compose 用 healthcheck 保证 MySQL、Redis 就绪后再启动应用。

只构建镜像：`docker build -t sky-take-out:1.0 .`

## 说明

- 数据库脚本：`sky-server/src/main/resources/sql/sky.sql`
- 敏感配置不入库，克隆后需自行创建 `application-dev.yml`
- 前端与小程序端随仓库提供，便于完整跑通流程
