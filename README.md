# Stock Trading Watchlist

Spring Boot application for managing stock watchlists and fetching live prices from Yahoo Finance.

Run locally:

```bash
mvn -f /Users/riazuddin/trading/pom.xml spring-boot:run
```

Build and run jar:

```bash
mvn -f /Users/riazuddin/trading/pom.xml -DskipTests package
java -jar /Users/riazuddin/trading/target/trading-0.0.1-SNAPSHOT.jar
```

UI: http://localhost:8080/
H2 Console: http://localhost:8080/h2-console
Hello endpoint: http://localhost:8080/hello
