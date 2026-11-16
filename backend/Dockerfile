# Étape 1 : Build Maven avec OpenJDK 25
FROM openjdk:25-jdk-slim AS builder
WORKDIR /app

# Copier la configuration Maven et le wrapper
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

# Télécharger les dépendances (cache Docker)
RUN ./mvnw dependency:go-offline -B || true

# Copier le code source et compiler le JAR (sans exécuter les tests pendant le build image)
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Étape 2 : Image d'exécution légère avec OpenJDK 25
FROM openjdk:25-jdk-slim
WORKDIR /app

# Copier le JAR généré depuis l'étape de build
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]