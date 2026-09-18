# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml và tải dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy mã nguồn và biên dịch ứng dụng
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file .jar đã build từ stage 1
COPY --from=build /app/target/*.jar app.jar

# Mở cổng mặc định
EXPOSE 8080
ENV PORT=8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
