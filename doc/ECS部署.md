# Stillgrove 阿里云 ECS 部署说明

此文档是部署操作模板；当前仓库尚未连接你的 ECS、域名或百炼账号，因此没有执行线上发布。

## 1. 推荐拓扑

```text
浏览器 HTTPS → ECS Nginx（前端静态文件 + /api 反向代理）
                         └→ Spring Boot API
                              ├→ RDS MySQL
                              ├→ Redis
                              ├→ Qdrant
                              ├→ 百炼 Chat / Embedding
                              └→ RocketMQ 5（可选启用；否则使用 MySQL Outbox 轮询）
```

实际采购与容量应按压测结果决定。面向大规模用户时，不建议把 MySQL、Redis、Qdrant、RocketMQ 和 API 都长期部署在同一台 ECS 上。优先在同一地域、同一 VPC 内连接后端依赖；数据库和消息队列不开放公网访问。

## 2. 发布前准备

- ECS 安装与项目匹配的 JDK，并准备 Nginx、域名与 HTTPS 证书。
- 准备 MySQL 8.0+、Redis、Qdrant；如启用 RocketMQ，准备 5.x Proxy 接入点、Topic 和 Consumer Group。
- 在百炼控制台创建 API Key，确认账号地域与 Base URL 配套。不要把 Key 写入 Git 或前端环境变量。
- 为 MySQL 建立独立应用账号并授予所需权限；配置备份、恢复演练和数据库连接加密。
- 安全组只开放 80/443 给公网。8080、3306、6379、6333、RocketMQ 端口只允许内网所需来源。

## 3. 构建

后端：

```bash
mvn -DskipTests package
```

前端：

```bash
npm ci
npm run build
```

将后端 JAR 放到 `/opt/stillgrove/api/`，前端 `dist/` 内容放到 `/var/www/stillgrove/`。发布时使用版本目录或原子替换，保留回滚版本。

## 4. 后端环境变量

设置 `SPRING_PROFILES_ACTIVE=prod`，并通过受控的服务环境提供：

```text
DB_URL=jdbc:mysql://<内网地址>:3306/stillgrove?connectionTimeZone=UTC
DB_USERNAME=<应用账号>
DB_PASSWORD=<数据库密码>
REDIS_HOST=<内网地址>
REDIS_PORT=6379
DASHSCOPE_API_KEY=<百炼密钥>
DASHSCOPE_BASE_URL=<与密钥地域匹配的 OpenAI 兼容 base URL>
DASHSCOPE_MODEL=qwen-plus
QDRANT_URL=http://<内网地址>:6333
QDRANT_API_KEY=<如启用鉴权>
STILLGROVE_CORS_ORIGIN=https://<你的域名>
SESSION_COOKIE_SECURE=true
ROCKETMQ_ENABLED=false
```

启用消息队列时再设置 `ROCKETMQ_ENABLED=true`、`ROCKETMQ_ENDPOINTS`、`ROCKETMQ_TOPIC`、`ROCKETMQ_CONSUMER_GROUP`。当前可选 RocketMQ 客户端配置未包含云实例 ACL 凭据；若所选实例要求 ACL，需在生产发布前补齐认证配置并完成联调。

## 5. Nginx 同域部署示例

将域名和证书路径替换为实际值：

```nginx
server {
    listen 443 ssl http2;
    server_name example.com;
    ssl_certificate /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    root /var/www/stillgrove;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffering off;
        proxy_read_timeout 180s;
    }
}
```

同域部署使 Session Cookie 和 CSRF 的浏览器行为更直接。上线前用真实域名验证注册、登录、刷新、SSE 回复、退出和注销。

## 6. 发布检查

1. Flyway 迁移成功；`/actuator/health` 为 `UP`。
2. Redis 中能看到 `stillgrove:session:sessions:*`；退出后对应 Session 消失。
3. 百炼 Chat 和 Embedding 均可调用，Qdrant Collection 维度与 Embedding 输出一致。
4. SSE 通过 Nginx 逐段到达浏览器，没有被代理缓冲。
5. 检查日志不含聊天正文、密码、Cookie 或 API Key。
6. 配置 MySQL 备份与恢复、监控告警、限流、容量与故障演练。

## 官方资料

- [阿里云 ECS 网站部署](https://help.aliyun.com/zh/ecs/user-guide/build-a-website/)
- [百炼 OpenAI 兼容 Chat](https://help.aliyun.com/zh/model-studio/qwen-api-via-openai-chat-completions/)
- [百炼 Embedding](https://help.aliyun.com/zh/model-studio/embedding-interfaces-compatible-with-openai/)
- [RDS MySQL SSL 连接](https://help.aliyun.com/zh/rds/apsaradb-rds-for-mysql/configure-a-cloud-certificate-to-enable-ssl-encryption)
