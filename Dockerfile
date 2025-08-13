# 使用一个更小的基础镜像
FROM debian:stable-slim

# 设置工作目录
WORKDIR /app

# 复制自定义的 JRE 和应用
COPY custom-jre /app/custom-jre
COPY target/gpt-api-sync-1.0.0.jar /app/gpt-api-sync.jar

# 暴露端口
EXPOSE 7000

# 启动应用
CMD ["/app/custom-jre/bin/java", "-jar", "/app/gpt-api-sync.jar"]