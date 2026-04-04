# ==========================================
# STAGE 1: Build (The "Heavy" Image)
# ==========================================
# We use a large image that includes Maven and the full JDK to compile our code
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build
# Copy only the pom.xml first to download dependencies and cache them
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the actual code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Production (The "Lightweight" Image)
# ==========================================
# We use a tiny Alpine Linux image running ONLY the JRE (Java Runtime Environment)
# We do not include Maven or source code here to keep the image secure and small!
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
# We extract the compiled JAR directly from STAGE 1
COPY --from=builder /build/target/*.jar app.jar

# Apply the JVM parameters we learned in Sprint 4!
ENTRYPOINT ["java", "-Xms512m", "-Xmx1g", "-XX:+UseG1GC", "-jar", "app.jar"]
