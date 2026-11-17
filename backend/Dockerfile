# Étape 1 : Build Maven avec Java 25
FROM openjdk:25-ea AS builder
WORKDIR /app

# Copier le wrapper Maven et le pom.xml
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

# Télécharger les dépendances
RUN ./mvnw dependency:go-offline -B || true

# Copier le code source et compiler le JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Étape 2 : Exécution avec Java 25
FROM openjdk:25-ea
WORKDIR /app

# Copier le JAR généré
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]