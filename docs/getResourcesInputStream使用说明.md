# getResourcesInputStream 使用说明

## 快速开始

```java
import com.anarckk.util.AutoConfigUtil;

---
InputStream is = AutoConfigUtil.getResourcesInputStream("config/app.yml");
String content = IOUtil.inputStreamToString(is);
```

## 加载优先级

1. **外置文件**（优先）：`{应用根目录}/{path}`
2. **内置文件**（兜底）：`classpath:/{path}`

## 环境变量覆盖

仅对内置的 YAML/Properties 文件生效。

### 规则

| 配置键 | 环境变量名 |
|--------|-----------|
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` |
| `db.port` | `DB_PORT` |

优先级：环境变量 > 系统属性 > 原始配置

### 使用效果

**原始 YAML：**
```yaml
db:
  host: localhost
  port: 3306
```

**执行命令：**
```bash
export DB_HOST=prod.server
export DB_PORT=5432
java -jar app.jar
```

**实际加载：**
```yaml
db:
  host: prod.server    # 已覆盖
  port: 5432           # 已覆盖（保持 Integer 类型）
```

## 注意事项

- 外置文件不会进行环境变量覆盖
- 记得关闭 `InputStream`，建议用 try-with-resources
- 路径统一使用 `/` 分隔符
