import React, { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

function Chart({ symbol }) {
  const [data, setData] = useState([]);

  useEffect(() => {
    // Mock data for demo; in production, fetch from backend
    const mockData = generateMockPriceData();
    setData(mockData);
  }, [symbol]);

  const generateMockPriceData = () => {
    const data = [];
    let price = 150;
    for (let i = 0; i < 24; i++) {
      price += (Math.random() - 0.5) * 5;
      data.push({
        time: `${i}:00`,
        price: parseFloat(price.toFixed(2))
      });
    }
    return data;
  };

  return (
    <div className="chart-container">
      <h3 className="text-lg font-semibold mb-4">24h Price Chart</h3>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={data} margin={{ top: 5, right: 30, left: 0, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#2a3a4a" />
          <XAxis dataKey="time" stroke="#999" />
          <YAxis stroke="#999" />
          <Tooltip contentStyle={{ backgroundColor: '#1a2332', border: '1px solid #3f51b5' }} />
          <Line type="monotone" dataKey="price" stroke="#3f51b5" dot={false} strokeWidth={2} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

export default Chart;
