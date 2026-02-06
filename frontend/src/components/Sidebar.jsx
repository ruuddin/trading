import React from 'react';

function Sidebar({ watchlists, onSelectSymbol }) {
  return (
    <div className="w-64 bg-darkLight border-r border-gray-700 p-4 overflow-y-auto">
      <h1 className="text-2xl font-bold mb-6 text-accent">Trading Dashboard</h1>
      
      <div className="space-y-1">
        {watchlists.map((list) => (
          <div key={list.id} className="mb-4">
            <h3 className="text-sm font-semibold text-gray-400 mb-2">{list.name}</h3>
            <div className="space-y-1">
              {list.stocks?.map((stock) => (
                <div
                  key={stock.id}
                  onClick={() => onSelectSymbol(stock.symbol)}
                  className="px-3 py-2 rounded text-sm hover:bg-accent/20 cursor-pointer transition"
                >
                  <div className="font-semibold">{stock.symbol}</div>
                  {stock.lastPrice && (
                    <div className={stock.lastPrice > 0 ? 'ticker-positive' : 'ticker-negative'}>
                      ${stock.lastPrice.toFixed(2)}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default Sidebar;
