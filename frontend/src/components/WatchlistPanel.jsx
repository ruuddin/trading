import React, { useState } from 'react';

function WatchlistPanel({ watchlists, prices, onRefresh }) {
  const [newSymbol, setNewSymbol] = useState('');
  const [selectedWatchlistId, setSelectedWatchlistId] = useState(null);

  const handleAddSymbol = async () => {
    if (!newSymbol || !selectedWatchlistId) return;
    
    try {
      const res = await fetch(`/api/watchlist/${selectedWatchlistId}/add`, {
        method: 'POST',
        body: new FormData(Object.entries({ symbol: newSymbol }).reduce((fd, [k, v]) => (fd.append(k, v), fd), new FormData())),
      });
      if (res.ok) {
        setNewSymbol('');
        onRefresh();
      }
    } catch (err) {
      console.error('Error adding symbol:', err);
    }
  };

  return (
    <div className="watchlist-container">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-semibold">Watchlists</h3>
        <button onClick={onRefresh} className="text-xs bg-accent/20 hover:bg-accent/40 px-2 py-1 rounded">
          Refresh
        </button>
      </div>

      {watchlists.length === 0 ? (
        <div className="text-center text-gray-500 py-4">No watchlists. Create one in the API.</div>
      ) : (
        <div className="space-y-4">
          {watchlists.map((list) => (
            <div key={list.id} className="bg-darker rounded p-3">
              <div className="font-semibold mb-2">{list.name}</div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-gray-400 border-b border-gray-700">
                    <th className="text-left py-1">Symbol</th>
                    <th className="text-right">Price</th>
                  </tr>
                </thead>
                <tbody>
                  {list.stocks?.map((stock) => (
                    <tr key={stock.id} className="border-b border-gray-800 hover:bg-darker/50">
                      <td className="py-2">{stock.symbol}</td>
                      <td className={`text-right font-semibold ${prices[stock.symbol] > 0 ? 'ticker-positive' : 'ticker-negative'}`}>
                        ${prices[stock.symbol]?.toFixed(2) || 'N/A'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default WatchlistPanel;
