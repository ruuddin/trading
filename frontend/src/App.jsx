import React, { useState, useEffect } from 'react';
import Sidebar from './components/Sidebar';
import Chart from './components/Chart';
import PriceTicker from './components/PriceTicker';
import WatchlistPanel from './components/WatchlistPanel';
import AdBanner from './components/AdBanner';
import './index.css';

function App() {
  const [watchlists, setWatchlists] = useState([]);
  const [selectedSymbol, setSelectedSymbol] = useState('AAPL');
  const [prices, setPrices] = useState({});

  useEffect(() => {
    fetchWatchlists();
    const interval = setInterval(fetchPrices, 30000); // Refresh every 30s
    return () => clearInterval(interval);
  }, []);

  const fetchWatchlists = async () => {
    try {
      const res = await fetch('/api/watchlist');
      if (!res.ok) return;
      const data = await res.json();
      setWatchlists(data);
      if (data.length > 0 && data[0].stocks?.length > 0) {
        setSelectedSymbol(data[0].stocks[0].symbol);
      }
    } catch (err) {
      console.error('Error fetching watchlists:', err);
    }
  };

  const fetchPrices = async () => {
    const symbols = new Set();
    watchlists.forEach(w => w.stocks?.forEach(s => symbols.add(s.symbol)));
    if (symbols.size === 0) return;
    try {
      const res = await fetch(`/api/prices?symbols=${[...symbols].join(',')}`);
      if (!res.ok) return;
      const data = await res.json();
      setPrices(data);
    } catch (err) {
      console.error('Error fetching prices:', err);
    }
  };

  return (
    <div className="flex h-screen bg-dark text-white">
      <Sidebar watchlists={watchlists} onSelectSymbol={setSelectedSymbol} />
      <div className="flex-1 flex flex-col overflow-y-auto">
        <AdBanner slot="header-ad" />
        <PriceTicker symbol={selectedSymbol} price={prices[selectedSymbol]} />
        <Chart symbol={selectedSymbol} />
        <AdBanner slot="middle-ad" />
        <WatchlistPanel watchlists={watchlists} prices={prices} onRefresh={fetchWatchlists} />
        <AdBanner slot="footer-ad" />
      </div>
    </div>
  );
}

export default App;
