package com.example.trading;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.trading.model.Stock;
import com.example.trading.model.User;
import com.example.trading.repository.StockRepository;
import com.example.trading.repository.UserRepository;

import java.math.BigDecimal;

@SpringBootApplication
public class TradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingApplication.class, args);
    }

    // seed sample stocks
    @Bean
    CommandLineRunner seed(StockRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(new Stock("AAPL", "Apple Inc.", new BigDecimal("170.00")));
                repo.save(new Stock("MSFT", "Microsoft Corp.", new BigDecimal("330.50")));
                repo.save(new Stock("TSLA", "Tesla, Inc.", new BigDecimal("240.12")));
            }
        };
    }

    // seed demo user
    @Bean
    CommandLineRunner seedUser(UserRepository userRepo, PasswordEncoder encoder) {
        return args -> {
            if (userRepo.count() == 0) {
                User demo = new User("demo", encoder.encode("demo123"));
                userRepo.save(demo);
            }
        };
    }
}
