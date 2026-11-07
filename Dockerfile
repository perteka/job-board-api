# =============================================================================
# Job Board & Application API — Dockerfile
# Multi-stage build: Java 17 + Spring Boot 3.5.7
# =============================================================================

# ---------------------------------------------------------------
# Aşama 1: Uygulama derleme
# ---------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Önce bağımlılıkları indir (layer cache için)
COPY pom.xml .
RUN mvn -q -e dependency:go-offline

# Sonra kaynak kodu kopyalayıp derle
COPY src ./src
RUN mvn -q -e package -DskipTests

# ---------------------------------------------------------------
# Aşama 2: Çalıştırılabilir imaj
# ---------------------------------------------------------------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Derlenmiş jar'ı kopyala
COPY --from=builder /build/target/*.jar app.jar

# Uygulama portu
EXPOSE 8080

# Sağlık kontrolü
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Uygulamayı çalıştır
ENTRYPOINT ["java", "-jar", "app.jar"]
