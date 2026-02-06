import React from 'react';

function PriceTicker({ symbol, price }) {
  const isPositive = price > 0;

  return (
    <div className="bg-darkLight p-6 border-b border-gray-700">
      <div className="flex items-baseline gap-4">
        <h2 className="text-4xl font-bold">{symbol}</h2>
        <div className="text-5xl font-bold">
          ${price ? price.toFixed(2) : 'N/A'}
        </div>
        {price && (
          <div className={`text-2xl font-semibold ${isPositive ? 'ticker-positive' : 'ticker-negative'}`}>
            {isPositive ? '+' : ''}{(price * 1.5).toFixed(2)}
          </div>
        )}
      </div>
    </div>
  );
}

export default PriceTicker;
