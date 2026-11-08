# Etapa de compilação. Os testes de integração com Testcontainers rodam no GitHub Actions,
# onde há um daemon Docker disponível; o build da imagem não inicia containers aninhados.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia primeiro o descritor para aproveitar o cache das dependências.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# Imagem final menor, sem Maven nem código-fonte.
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring --home-dir /app spring
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar

USER spring:spring
ENV PORT=8080
EXPOSE 8080

# Render fornece PORT; JAVA_OPTS permite ajustes sem reconstruir a imagem.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
