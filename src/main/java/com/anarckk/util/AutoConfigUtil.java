package com.anarckk.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import java.io.ByteArrayOutputStream;

import org.yaml.snakeyaml.Yaml;

import static com.anarckk.util.IOUtil.inputStreamToString;

/**
 * 资源文件读取测试类
 * 用于演示如何从外部文件系统或内部classpath读取资源文件
 * 支持从jar包内或外部目录读取配置文件
 * Created by anarckk on 2025-09-26.
 */
@Slf4j
public class AutoConfigUtil {
    /**
     * 获取资源文件输入流
     * 优先从外部文件系统加载配置文件，如果不存在则从classpath内部加载
     * 对于内置的YAML文件，会检查环境变量并覆盖对应的配置值
     *
     * @param path 资源文件路径
     * @return 资源文件输入流
     * @throws IOException 如果文件读取出错
     */
    public static InputStream getResourcesInputStream(String path) throws IOException {
        File f = new File(getPath() + File.separator + path);
        log.debug("判断文件: {} 存在则为外置", f.getAbsolutePath());
        if (f.exists()) {
            log.debug("读取外置文件: {}", f.getCanonicalPath());
            return Files.newInputStream(f.toPath());
        } else {
            log.debug("读取内置文件: {}", path);
            InputStream originalStream = AutoConfigUtil.class.getResourceAsStream("/" + path);
            // 如果是YAML文件，处理环境变量覆盖
            if (path.toLowerCase().endsWith(".yml") || path.toLowerCase().endsWith(".yaml")) {
                return processYamlWithEnvVariables(originalStream, path);
            }
            // 如果是properties文件，处理环境变量覆盖
            if (path.toLowerCase().endsWith(".properties")) {
                return processPropertiesWithEnvVariables(originalStream, path);
            }
            return originalStream;
        }
    }

    /**
     * 获取应用程序路径
     * 根据运行环境确定应用程序的实际路径，支持从IDE或jar包中运行的情况
     * @return 返回应用程序的根路径
     */
    private static String getPath() {
        // 获取当前类所在的类路径（classpath）的物理路径
        // 1. ReadResourcesFileTest.class 获取当前类的Class对象
        // 2. getProtectionDomain() 获取该类的保护域，包含代码来源等安全相关信息
        // 3. getCodeSource() 获取代码来源，即类文件的来源位置
        // 4. getLocation() 获取代码来源的URL位置
        // 5. getPath() 将URL转换为文件路径字符串
        // 最终结果是获取到当前类所在JAR包或class文件目录的绝对路径，常用于定位资源文件
        String path = AutoConfigUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        log.debug("看下path是什么: {}", path);
        if (path.startsWith("file:")) {
            path = path.substring("file:".length());
        }
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            path = path.substring(1);
        }
        if (path.contains(".jar")) {
            path = path.substring(0, path.indexOf(".jar"));
            String jarResult = path.substring(0, path.lastIndexOf("/"));
            log.debug("getPath()函数执行结果(JAR包): {}", jarResult);
            return jarResult;
        }
        String result = path.replace("target/classes/", "");
        log.debug("getPath()函数执行结果(普通路径): {}", result);
        return result;
    }

    /**
     * 处理YAML文件，检查环境变量并覆盖对应的配置值
     *
     * @param originalStream 原始输入流
     * @param filePath 文件路径
     * @return 处理后的输入流
     * @throws IOException 如果处理过程中出错
     */
    private static InputStream processYamlWithEnvVariables(InputStream originalStream, String filePath) throws IOException {
        try {
            // 读取原始内容
            String originalContent = inputStreamToString(originalStream);
            originalStream.close();

            // 使用SnakeYAML解析YAML内容
            Yaml yaml = new Yaml();
            Map<String, Object> yamlData = yaml.load(originalContent);

            // 检查环境变量并覆盖
            boolean hasEnvOverrides = applyEnvOverridesToYamlData(yamlData, "");

            // 如果有环境变量覆盖，重新构建YAML内容
            if (hasEnvOverrides) {
                String modifiedContent = yaml.dump(yamlData);
                log.debug("应用环境变量覆盖后的YAML内容:\n{}", modifiedContent);
                return new ByteArrayInputStream(modifiedContent.getBytes(StandardCharsets.UTF_8));
            }

            // 没有环境变量覆盖，返回原始内容
            return new ByteArrayInputStream(originalContent.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            log.warn("处理YAML文件环境变量覆盖失败: {}", e.getMessage());
            // 如果处理失败，关闭原始流
            try {
                originalStream.close();
            } catch (IOException ioe) {
                log.debug("关闭原始流失败: {}", ioe.getMessage());
            }
            // 返回空流，因为无法读取原始内容
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    /**
     * 递归应用环境变量覆盖到YAML数据
     *
     * @param data YAML数据
     * @param currentPath 当前路径（用于构建完整键名）
     * @return 是否有环境变量覆盖
     */
    private static boolean applyEnvOverridesToYamlData(Map<String, Object> data, String currentPath) {
        boolean hasOverrides = false;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String fullPath = currentPath.isEmpty() ? key : currentPath + "." + key;

            // FIXME: value 可能是Integer类型
            if (value instanceof Map) {
                // 递归处理嵌套的Map
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                boolean nestedHasOverrides = applyEnvOverridesToYamlData(nestedMap, fullPath);
                hasOverrides = hasOverrides || nestedHasOverrides;
            } else if (value instanceof String) {
                // 检查系统属性和环境变量（优先使用环境变量）
                String envKey = fullPath.replace(".", "_").toUpperCase();
                String envValue = System.getenv(envKey);

                // 如果环境变量不存在，检查系统属性
                if (envValue == null || envValue.trim().isEmpty()) {
                    envValue = System.getProperty(envKey);
                }

                if (envValue != null && !envValue.trim().isEmpty()) {
                    log.debug("发现配置覆盖 {} = {}，覆盖YAML配置 {}", envKey, envValue, fullPath);
                    data.put(key, envValue);
                    hasOverrides = true;
                }
            }
        }

        return hasOverrides;
    }

    /**
     * 处理properties文件，检查环境变量并覆盖对应的配置值
     *
     * @param originalStream 原始输入流
     * @param filePath 文件路径
     * @return 处理后的输入流
     * @throws IOException 如果处理过程中出错
     */
    private static InputStream processPropertiesWithEnvVariables(InputStream originalStream, String filePath) throws IOException {
        try {
            Properties props = new Properties();
            props.load(originalStream);
            originalStream.close();

            boolean hasEnvOverrides = false;
            for (String key : props.stringPropertyNames()) {
                String envKey = key.replace(".", "_").toUpperCase();
                String envValue = System.getenv(envKey);

                // 如果环境变量不存在，检查系统属性
                if (envValue == null || envValue.trim().isEmpty()) {
                    envValue = System.getProperty(envKey);
                }

                if (envValue != null && !envValue.trim().isEmpty()) {
                    log.debug("发现配置覆盖 {} = {}，覆盖properties配置 {}", envKey, envValue, key);
                    props.setProperty(key, envValue);
                    hasEnvOverrides = true;
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            props.store(baos, null);
            byte[] bytes = baos.toByteArray();
            baos.close();
            if (hasEnvOverrides) {
                log.debug("应用环境变量覆盖后的properties内容: {}", new String(bytes, StandardCharsets.UTF_8));
            }
            return new ByteArrayInputStream(bytes);
        } catch (Exception e) {
            log.warn("处理properties文件环境变量覆盖失败: {}", e.getMessage());
            // 如果处理失败，关闭原始流
            try {
                originalStream.close();
            } catch (IOException ioe) {
                log.debug("关闭原始流失败: {}", ioe.getMessage());
            }
            // 返回空流，因为无法读取原始内容
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    /**
     * 主方法，演示资源文件读取
     * 读取test.txt文件并输出其内容
     *
     * @param args 命令行参数
     * @throws IOException 如果文件读取出错
     */
    public static void main(String[] args) throws IOException {
        InputStream is = getResourcesInputStream("test-config.yml");
        String content = inputStreamToString(is);
        log.info("读取出的数据是: " + content);
    }
}