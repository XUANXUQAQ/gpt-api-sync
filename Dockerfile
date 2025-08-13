FROM eclipse-temurin:21-jre

# 设置工作目录
WORKDIR /app

# 将 target 文件夹下的 jar 包复制到镜像中
COPY target/gpt-api-sync-1.0.0.jar /app/gpt-api-sync.jar

# 暴露端口
EXPOSE 7000

# 启动应用
CMD ["java", "-jar", "/app/gpt-api-sync.jar"]