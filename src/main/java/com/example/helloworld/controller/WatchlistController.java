package com.example.helloworld.controller;

import com.example.helloworld.model.StockItem;
import com.example.helloworld.model.Watchlist;
import com.example.helloworld.repository.StockItemRepository;
import com.example.helloworld.repository.WatchlistRepository;
import com.example.helloworld.service.StockPriceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class WatchlistController {
    private final WatchlistRepository watchlistRepo;
    private final StockItemRepository stockRepo;
    private final StockPriceService priceService;

    public WatchlistController(WatchlistRepository w, StockItemRepository s, StockPriceService p) {
        this.watchlistRepo = w; this.stockRepo = s; this.priceService = p;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Watchlist> lists = watchlistRepo.findAll();
        model.addAttribute("lists", lists);
        model.addAttribute("newList", new Watchlist());
        return "index";
    }

    @PostMapping("/watchlist")
    public String createWatchlist(@ModelAttribute Watchlist newList) {
        if (newList.getName() != null && !newList.getName().isBlank()) watchlistRepo.save(newList);
        return "redirect:/";
    }

    @GetMapping("/watchlist/{id}")
    public String viewWatchlist(@PathVariable Long id, Model model) {
        Optional<Watchlist> opt = watchlistRepo.findById(id);
        if (opt.isEmpty()) return "redirect:/";
        Watchlist w = opt.get();

        // fetch prices
        List<String> symbols = w.getStocks().stream().map(StockItem::getSymbol).collect(Collectors.toList());
        Map<String, BigDecimal> prices = priceService.fetchPrices(symbols);
        for (StockItem s : w.getStocks()) {
            BigDecimal p = prices.get(s.getSymbol().toUpperCase());
            if (p != null) s.setLastPrice(p);
        }

        model.addAttribute("watchlist", w);
        model.addAttribute("newStock", new StockItem());
        return "watchlist";
    }

    @PostMapping("/watchlist/{id}/add")
    public String addSymbol(@PathVariable Long id, @ModelAttribute StockItem newStock) {
        Optional<Watchlist> opt = watchlistRepo.findById(id);
        if (opt.isEmpty()) return "redirect:/";
        Watchlist w = opt.get();
        String sym = newStock.getSymbol();
        if (sym != null && !sym.isBlank()) {
            StockItem s = new StockItem(sym.trim().toUpperCase());
            w.addStock(s);
            watchlistRepo.save(w);
        }
        return "redirect:/watchlist/" + id;
    }

    @PostMapping("/watchlist/{id}/remove/{stockId}")
    public String removeSymbol(@PathVariable Long id, @PathVariable Long stockId) {
        Optional<Watchlist> opt = watchlistRepo.findById(id);
        Optional<StockItem> so = stockRepo.findById(stockId);
        if (opt.isPresent() && so.isPresent()) {
            Watchlist w = opt.get();
            StockItem s = so.get();
            w.removeStock(s);
            watchlistRepo.save(w);
        }
        return "redirect:/watchlist/" + id;
    }
}
