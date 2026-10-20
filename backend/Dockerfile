# Estágio 1: Build da aplicação Spring Boot com Maven e Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar ficheiro pom.xml e descarregar dependências para cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código-fonte e compilar o pacote jar de produção
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio 2: Imagem leve de execução com JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Variáveis de ambiente com valores por defeito
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
