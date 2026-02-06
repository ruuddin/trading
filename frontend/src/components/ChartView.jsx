import React, { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import SymbolSearch from './SymbolSearch';

export default function ChartView({ symbol, timeRange, onTimeRangeChange, price }) {
  const [chartData, setChartData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showSearch, setShowSearch] = useState(false);

  const timeRanges = [
    { label: '1m', value: '1d', interval: '1m' },
    { label: '5m', value: '5d', interval: '5m' },
    { label: '15m', value: '1mo', interval: '15m' },
    { label: '1h', value: '3mo', interval: '1h' },
    { label: '1d', value: '1y', interval: '1d' },
  ];

  useEffect(() => {
    if (!symbol) return;
    fetchChartData();
  }, [symbol, timeRange]);

  const fetchChartData = async () => {
    setLoading(true);
    try {
      const selectedRange = timeRanges.find(r => r.label === timeRange);
      const params = selectedRange ? selectedRange : timeRanges[3];
      
      const res = await fetch(`/api/chart/history?symbol=${symbol}&interval=${params.interval}&range=${params.value}`);
      if (res.ok) {
        const data = await res.json();
        setChartData(data.map((point, idx) => ({
          ...point,
          displayPrice: point.price.toString(),
          displayTime: new Date(point.timestamp).toLocaleTimeString()
        })));
      }
    } catch (err) {
      console.error('Error fetching chart data:', err);
      setChartData([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex-1 flex flex-col bg-darkLight p-4 overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-4">
          <button
            onClick={() => setShowSearch(!showSearch)}
            className="px-4 py-2 bg-accent text-white rounded-lg hover:bg-opacity-80 transition"
          >
            🔍 Search: {symbol}
          </button>
          {showSearch && (
            <SymbolSearch
              onSelectSymbol={(sym) => {
                onTimeRangeChange('1d');
                setShowSearch(false);
              }}
              onClose={() => setShowSearch(false)}
            />
          )}
        </div>

        <div className="text-right">
          <div className="text-4xl font-bold text-accent">${price || 'N/A'}</div>
          <div className="text-sm text-gray-400">{symbol}</div>
        </div>
      </div>

      {/* Time Range Buttons */}
      <div className="flex gap-2 mb-4 flex-wrap">
        {timeRanges.map(range => (
          <button
            key={range.label}
            onClick={() => onTimeRangeChange(range.label)}
            className={`px-3 py-1 rounded text-sm font-medium transition ${
              timeRange === range.label
                ? 'bg-accent text-white'
                : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
            }`}
          >
            {range.label}
          </button>
        ))}
      </div>

      {/* Chart */}
      <div className="flex-1 relative">
        {loading && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/50 rounded-lg z-10">
            <div className="text-white">Loading...</div>
          </div>
        )}
        
        {chartData.length > 0 ? (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#444" />
              <XAxis
                dataKey="displayTime"
                stroke="#888"
                tick={{ fontSize: 12 }}
                interval={Math.floor(chartData.length / 10)}
              />
              <YAxis stroke="#888" tick={{ fontSize: 12 }} />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#1a1f2e',
                  border: '1px solid #444',
                  borderRadius: '0.5rem',
                }}
                labelStyle={{ color: '#fff' }}
              />
              <Line
                type="monotone"
                dataKey="price"
                stroke="#3f51b5"
                dot={false}
                strokeWidth={2}
              />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <div className="flex items-center justify-center h-full text-gray-400">
            {loading ? 'Loading chart data...' : 'No chart data available'}
          </div>
        )}
      </div>
    </div>
  );
}
