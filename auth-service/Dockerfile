# ---------- Stage 1: Build the jar ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# pom.xml first so this layer caches as long as dependencies don't change
COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ---------- Stage 2: Extract layered jar ----------
FROM eclipse-temurin:21-jdk AS extractor
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ---------- Stage 3: Runtime image ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

EXPOSE 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
