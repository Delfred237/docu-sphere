# ==============================================================================
# Stage 1: Build - Compile l'application
# ==============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copier uniquement les fichiers de dépendances pour le cache Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source et compiler
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Stage 2: Runtime - Image minimale pour la production
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Métadonnées de l'image
LABEL maintainer="DocuSphere Team <delfredtene17@gmail.com>"
LABEL description="DocuSphere Backend - Document Management Platform"
LABEL version="1.0.0"

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S docusphere && adduser -S docusphere -G docusphere

# Créer les dossiers nécessaires
RUN mkdir -p /app/logs /app/storage && \
    chown -R docusphere:docusphere /app

# Répertoire de travail
WORKDIR /app

# Copier uniquement le JAR compilé depuis le stage builder
COPY --from=builder /build/target/*.jar app.jar

# Donner les permissions à l'utilisateur non-root
RUN chown -R docusphere:docusphere /app

# Utiliser l'utilisateur non-root
USER docusphere

# Variables d'environnement par défaut
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    SPRING_PROFILES_ACTIVE=prod \
    STORAGE_PATH=/app/storage \
    TZ=UTC

# Exposer le port de l'application
EXPOSE 8080

# Health check pour vérifier que l'application est vivante
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

# Point d'entrée
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]