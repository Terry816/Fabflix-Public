# ---------- Stage 1: Build React frontend ----------
FROM node:24-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm install
COPY frontend ./
RUN npm run build

# ---------- Stage 2: Build Spring Boot API ----------
FROM maven:3.9-eclipse-temurin-21 AS backend-builder
WORKDIR /app

COPY pom.xml ./
COPY src ./src
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static
RUN mvn clean package -DskipTests

# ---------- Stage 3: Runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=backend-builder /app/target/fabflix-0.0.1-SNAPSHOT.jar /app/fabflix.jar
COPY docker-entrypoint.sh /usr/local/bin/fabflix-entrypoint.sh
RUN chmod +x /usr/local/bin/fabflix-entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["fabflix-entrypoint.sh"]
