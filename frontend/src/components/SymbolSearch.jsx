import React, { useState, useEffect } from 'react';

export default function SymbolSearch({ onSelectSymbol, onClose }) {
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (query.length < 1) {
      setSuggestions([]);
      return;
    }

    setLoading(true);
    const timer = setTimeout(async () => {
      try {
        const res = await fetch(`/api/search/symbols?q=${query}`);
        if (res.ok) {
          const data = await res.json();
          setSuggestions(data);
        }
      } catch (err) {
        console.error('Error searching symbols:', err);
      } finally {
        setLoading(false);
      }
    }, 300); // Debounce 300ms

    return () => clearTimeout(timer);
  }, [query]);

  const handleSelect = (symbol) => {
    onSelectSymbol(symbol);
    setQuery('');
    setSuggestions([]);
    onClose?.();
  };

  return (
    <div className="absolute top-12 left-0 right-0 bg-darkLight rounded-lg border border-gray-700 shadow-lg z-50">
      <input
        type="text"
        placeholder="Search symbol (e.g., AAPL, MSFT)..."
        value={query}
        onChange={(e) => setQuery(e.target.value.toUpperCase())}
        className="w-full px-4 py-2 bg-darkLight text-white border-b border-gray-700 focus:outline-none"
        autoFocus
      />
      
      {loading && (
        <div className="px-4 py-3 text-gray-400 text-sm">Searching...</div>
      )}
      
      {suggestions.length > 0 && (
        <ul className="max-h-64 overflow-y-auto">
          {suggestions.map((item, idx) => (
            <li
              key={idx}
              onClick={() => handleSelect(item.symbol)}
              className="px-4 py-2 hover:bg-gray-800 cursor-pointer border-b border-gray-800"
            >
              <div className="font-semibold">{item.symbol}</div>
              <div className="text-xs text-gray-400">{item.displayName}</div>
            </li>
          ))}
        </ul>
      )}
      
      {query && suggestions.length === 0 && !loading && (
        <div className="px-4 py-3 text-gray-400 text-sm">No results found</div>
      )}
    </div>
  );
}
