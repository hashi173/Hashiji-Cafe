# Sử dụng Eclipse Temurin (thay cho OpenJDK cũ đã bị khai tử trên Docker Hub)
FROM maven:3.8.7-eclipse-temurin-17 AS build
WORKDIR /app

# Copy toàn bộ code vào container
COPY . .

# Build file JAR (Bỏ qua chạy test để build nhanh hơn)
RUN mvn clean package -DskipTests

# Bước 2: Chạy ứng dụng bằng Eclipse Temurin JDK 17
FROM eclipse-temurin:17-jdk-focal
WORKDIR /app

# Copy file JAR đã build từ bước trước sang container mới
COPY --from=build /app/target/*.jar app.jar

# Mở cổng 8080 (cổng mặc định của Spring Boot)
EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
