import React, { useState, useEffect } from 'react';
import WatchlistSidebar from './components/WatchlistSidebar';
import ChartView from './components/ChartView';
import AdBanner from './components/AdBanner';
import './index.css';

function App() {
  const [watchlists, setWatchlists] = useState([]);
  const [selectedWatchlistId, setSelectedWatchlistId] = useState(null);
  const [selectedSymbol, setSelectedSymbol] = useState('AAPL');
  const [prices, setPrices] = useState({});
  const [timeRange, setTimeRange] = useState('1d');

  useEffect(() => {
    fetchWatchlists();
    const interval = setInterval(fetchPrices, 5000); // Refresh every 5 seconds
    return () => clearInterval(interval);
  }, [watchlists]);

  const fetchWatchlists = async () => {
    try {
      const res = await fetch('/api/watchlist');
      if (!res.ok) return;
      const data = await res.json();
      setWatchlists(data);
      if (data.length > 0 && !selectedWatchlistId) {
        setSelectedWatchlistId(data[0].id);
        if (data[0].stocks?.length > 0) {
          setSelectedSymbol(data[0].stocks[0].symbol);
        }
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
    <div className="flex h-screen bg-dark text-white flex-col">
      <AdBanner slot="header-ad" />
      <div className="flex flex-1 overflow-hidden">
        {/* Left Sidebar - 20% */}
        <div className="w-1/5 border-r border-gray-700 overflow-y-auto">
          <WatchlistSidebar 
            watchlists={watchlists} 
            onSelectSymbol={setSelectedSymbol}
            onSelectWatchlist={setSelectedWatchlistId}
            selectedWatchlistId={selectedWatchlistId}
            prices={prices}
            onRefresh={fetchWatchlists}
          />
        </div>
        
        {/* Right Content Area - 80% */}
        <div className="flex-1 flex flex-col overflow-hidden">
          <ChartView 
            symbol={selectedSymbol} 
            timeRange={timeRange}
            onTimeRangeChange={setTimeRange}
            price={prices[selectedSymbol]}
          />
          <AdBanner slot="middle-ad" />
        </div>
      </div>
      <AdBanner slot="footer-ad" />
    </div>
  );
}

export default App;
