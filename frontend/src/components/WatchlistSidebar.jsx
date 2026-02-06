import React, { useState } from 'react';

export default function WatchlistSidebar({
  watchlists,
  onSelectSymbol,
  onSelectWatchlist,
  selectedWatchlistId,
  prices,
  onRefresh,
}) {
  const [newSymbol, setNewSymbol] = useState('');
  const [showAddForm, setShowAddForm] = useState(null);

  const selectedWatchlist = watchlists.find(w => w.id === selectedWatchlistId);

  const handleAddSymbol = async (watchlistId) => {
    if (!newSymbol.trim()) return;

    try {
      const res = await fetch(`/api/watchlist/${watchlistId}/add-symbol`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ symbol: newSymbol.toUpperCase() }),
      });

      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          setNewSymbol('');
          setShowAddForm(null);
          onRefresh();
        } else {
          alert(data.error || 'Failed to add symbol');
        }
      }
    } catch (err) {
      console.error('Error adding symbol:', err);
    }
  };

  const handleCreateWatchlist = async () => {
    const name = prompt('Enter watchlist name (e.g., Tech Stocks):');
    if (!name) return;

    try {
      const res = await fetch('/api/watchlist', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name }),
      });

      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          onRefresh();
        } else {
          alert(data.error || 'Failed to create watchlist');
        }
      }
    } catch (err) {
      console.error('Error creating watchlist:', err);
    }
  };

  return (
    <div className="h-full flex flex-col bg-dark overflow-hidden">
      {/* Header */}
      <div className="border-b border-gray-700 p-4">
        <h2 className="text-xl font-bold text-accent mb-3">Watchlists</h2>
        <div className="flex gap-2">
          <button
            onClick={handleCreateWatchlist}
            disabled={watchlists.length >= 10}
            className={`flex-1 px-2 py-1 rounded text-sm font-medium transition ${
              watchlists.length >= 10
                ? 'bg-gray-700 text-gray-500 cursor-not-allowed'
                : 'bg-accent text-white hover:bg-opacity-80'
            }`}
            title={watchlists.length >= 10 ? 'Max 10 watchlists' : ''}
          >
            + New
          </button>
          <button
            onClick={onRefresh}
            className="px-2 py-1 bg-gray-700 text-white rounded text-sm hover:bg-gray-600 transition"
          >
            ↻
          </button>
        </div>
        {watchlists.length >= 10 && (
          <div className="text-xs text-yellow-400 mt-2">Max 10 watchlists reached</div>
        )}
      </div>

      {/* Watchlists List */}
      <div className="flex-1 overflow-y-auto">
        {watchlists.length === 0 ? (
          <div className="p-4 text-center text-gray-400 text-sm">
            No watchlists yet. Create one to get started!
          </div>
        ) : (
          watchlists.map(watchlist => (
            <div key={watchlist.id} className="border-b border-gray-800">
              {/* Watchlist Header */}
              <button
                onClick={() => onSelectWatchlist(watchlist.id)}
                className={`w-full px-4 py-2 text-left font-semibold transition ${
                  selectedWatchlistId === watchlist.id
                    ? 'bg-gray-800 text-accent border-l-2 border-accent'
                    : 'hover:bg-gray-900 text-white'
                }`}
              >
                {watchlist.name}
                <span className="text-xs text-gray-400 ml-2">
                  ({watchlist.stocks?.length || 0}/20)
                </span>
              </button>

              {/* Stocks List */}
              {selectedWatchlistId === watchlist.id && (
                <>
                  <div className="bg-gray-900 px-4 py-2">
                    {watchlist.stocks && watchlist.stocks.length > 0 ? (
                      <ul className="space-y-1">
                        {watchlist.stocks.map(stock => (
                          <li
                            key={stock.id}
                            onClick={() => onSelectSymbol(stock.symbol)}
                            className="px-2 py-1 rounded cursor-pointer hover:bg-gray-800 transition flex justify-between items-center text-sm"
                          >
                            <span className="font-medium text-white">{stock.symbol}</span>
                            <span className={`text-sm ${
                              prices[stock.symbol]
                                ? parseFloat(prices[stock.symbol]) > 0
                                  ? 'text-green-400'
                                  : 'text-red-400'
                                : 'text-gray-400'
                            }`}>
                              ${prices[stock.symbol]?.toString() || 'Loading...'}
                            </span>
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="text-gray-500 text-xs">No symbols yet</p>
                    )}

                    {/* Add Symbol Form */}
                    {showAddForm === watchlist.id ? (
                      <div className="mt-2 flex gap-1">
                        <input
                          type="text"
                          placeholder="Enter symbol"
                          value={newSymbol}
                          onChange={(e) => setNewSymbol(e.target.value.toUpperCase())}
                          onKeyPress={(e) => {
                            if (e.key === 'Enter') {
                              handleAddSymbol(watchlist.id);
                            }
                          }}
                          className="flex-1 px-2 py-1 bg-darkLight border border-gray-700 rounded text-white text-sm focus:outline-none focus:border-accent"
                          autoFocus
                        />
                        <button
                          onClick={() => handleAddSymbol(watchlist.id)}
                          disabled={watchlist.stocks?.length >= 20}
                          className={`px-2 py-1 rounded text-sm font-medium transition ${
                            watchlist.stocks?.length >= 20
                              ? 'bg-gray-700 text-gray-500 cursor-not-allowed'
                              : 'bg-accent text-white hover:bg-opacity-80'
                          }`}
                          title={watchlist.stocks?.length >= 20 ? 'Max 20 symbols' : ''}
                        >
                          Add
                        </button>
                      </div>
                    ) : (
                      <button
                        onClick={() => setShowAddForm(watchlist.id)}
                        disabled={watchlist.stocks?.length >= 20}
                        className={`w-full mt-2 px-2 py-1 rounded text-sm font-medium transition ${
                          watchlist.stocks?.length >= 20
                            ? 'bg-gray-700 text-gray-500 cursor-not-allowed'
                            : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                        }`}
                        title={watchlist.stocks?.length >= 20 ? 'Max 20 symbols' : ''}
                      >
                        + Add Symbol
                      </button>
                    )}
                    {watchlist.stocks?.length >= 20 && (
                      <div className="text-xs text-yellow-400 mt-1">Max 20 symbols reached</div>
                    )}
                  </div>
                </>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
