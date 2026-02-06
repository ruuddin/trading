# Hello World Spring Boot

Simple Spring Boot "Hello, World!" example.

Run locally:

```bash
mvn -f /Users/riazuddin/hello-world/pom.xml spring-boot:run
```

Build and run jar:

```bash
mvn -f /Users/riazuddin/hello-world/pom.xml -DskipTests package
java -jar /Users/riazuddin/hello-world/target/hello-world-0.0.1-SNAPSHOT.jar
```

Endpoint: `GET /` → "Hello, World!"
