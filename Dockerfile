# Dockerfile Multi-stage pour Spring Boot Fil Rouge
# Étape 1 : Build de l'application
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Optimisation du cache des dépendances Maven
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Compilation et packaging
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Étape 2 : Image d'exécution minimale et sécurisée
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Création d'un utilisateur non-root pour des raisons de sécurité
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copie du jar compilé depuis le stage builder
COPY --from=builder /workspace/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
