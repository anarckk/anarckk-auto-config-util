# YAML环境变量覆盖功能增强说明

## 功能概述

本增强为`ReadResourcesFileTest`类的`getInputStreamFromResources`方法添加了YAML文件环境变量覆盖功能，支持根据系统环境变量动态覆盖YAML配置文件中的值。

## 核心特性

### 1. 智能文件识别
- 自动识别`.yml`和`.yaml`文件
- 区分内置文件和外置文件，只有内置文件才进行环境变量覆盖

### 2. 配置覆盖机制
- 将YAML中的点分隔键名转换为配置键格式（如`jdbc.url` → `JDBC_URL`）
- **优先检查系统环境变量**，如果存在则使用环境变量值
- **其次检查系统属性**，如果环境变量不存在则使用系统属性值
- 如果存在配置覆盖，则用配置值覆盖YAML中的配置值

### 3. 嵌套结构支持
- 支持处理嵌套的YAML结构
- 递归检查所有层级的配置项

## 使用示例

### YAML配置文件示例
```yaml
# config.yml
jdbc:
  url: localhost:3306
  username: root
  password: password123

server:
  port: 8080
  host: 127.0.0.1

logging:
  level: INFO
  file: /var/log/app.log
```

### 配置设置方式

#### 方式1：环境变量（生产环境推荐）
```bash
# Linux/macOS
export JDBC_URL="mysql://prod-db:3306"
export JDBC_USERNAME="admin"
export SERVER_PORT="9090"

# Windows
set JDBC_URL=mysql://prod-db:3306
set JDBC_USERNAME=admin
set SERVER_PORT=9090
```

#### 方式2：系统属性（开发/测试环境）
```java
// 在Java代码中设置系统属性
System.setProperty("JDBC_URL", "mysql://prod-db:3306");
System.setProperty("SERVER_PORT", "9090");

// 或者在启动JVM时设置
// java -DJDBC_URL=mysql://prod-db:3306 -DSERVER_PORT=9090 -jar app.jar
```

### Java代码使用
```java
// 读取YAML文件，自动应用配置覆盖
InputStream is = GetResourcesInputStreamTest.getResourcesInputStream("config.yml");

// 如果设置了环境变量或系统属性，YAML中的配置会被自动覆盖：
// jdbc.url: localhost:3306 → mysql://prod-db:3306
// server.port: 8080 → 9090
```

## 配置覆盖规则

### 配置键名转换规则

YAML配置键名到配置键的转换规则：

| YAML键名 | 配置键名 |
|---------|-----------|
| `jdbc.url` | `JDBC_URL` |
| `server.port` | `SERVER_PORT` |
| `logging.level` | `LOGGING_LEVEL` |

转换规则：
1. 将点号`.`替换为下划线`_`
2. 转换为大写字母

### 配置源优先级

配置覆盖按照以下优先级顺序检查：

1. **环境变量**（最高优先级）
   ```bash
   # Linux/macOS
   export JDBC_URL="mysql://prod-db:3306"
   
   # Windows  
   set JDBC_URL=mysql://prod-db:3306
   ```

2. **系统属性**（次优先级）
   ```java
   System.setProperty("JDBC_URL", "mysql://prod-db:3306");
   ```

3. **YAML文件默认值**（最低优先级）
   ```yaml
   jdbc:
     url: localhost:3306
   ```

## 文件位置判断逻辑

### 内置文件（应用环境变量覆盖）
- 文件存在于应用程序目录中
- 路径：`{app_dir}/config.yml`

### 外置文件（不应用环境变量覆盖）
- 文件存在于classpath中（如JAR包内）
- 路径：`classpath:/config.yml`

## 错误处理

- 如果YAML解析失败，返回原始文件内容
- 如果环境变量读取失败，保持YAML原始值
- 所有操作都有详细的日志记录

## 依赖要求

- Java 8+
- SnakeYAML 2.2+

## 最佳实践

1. **开发环境**：使用默认的YAML配置
2. **测试环境**：通过环境变量覆盖部分配置
3. **生产环境**：完全通过环境变量管理敏感配置

## 优势

1. **安全性**：敏感信息（如密码）可以通过环境变量管理，避免写入配置文件
2. **灵活性**：不同环境使用不同的配置，无需修改配置文件
3. **标准化**：符合12要素应用原则，支持容器化部署
4. **兼容性**：与Spring Boot等主流框架的环境变量覆盖机制保持一致

## 注意事项

- 环境变量值会完全覆盖YAML中的对应值
- 只支持字符串类型的配置项覆盖
- 环境变量名必须为大写，使用下划线分隔