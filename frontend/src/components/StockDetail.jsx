import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ComposedChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Area, Customized, Cell } from 'recharts';
import { getLogoUrl, getInitialsBadge } from '../services/logoService';
import FeatureGate from './FeatureGate';
import AlertsPanel from './AlertsPanel';
import { subscribeToQuoteStream } from '../services/quoteStream';

const INTERVAL_OPTIONS = [
  { label: '1D', value: '1D', apiInterval: 'daily', maxAgeDays: 1 },
  { label: '1W', value: '1W', apiInterval: 'daily', maxAgeDays: 7 },
  { label: '1M', value: '1M', apiInterval: 'daily', maxAgeDays: 30 },
  { label: '1Y', value: '1Y', apiInterval: 'daily', maxAgeDays: 365 },
  { label: '3Y', value: '3Y', apiInterval: 'weekly', maxAgeDays: 365 * 3 },
  { label: '5Y', value: '5Y', apiInterval: 'weekly', maxAgeDays: 365 * 5 },
  { label: '10Y', value: '10Y', apiInterval: 'monthly', maxAgeDays: 365 * 10 },
  { label: 'All', value: 'ALL', apiInterval: 'monthly', maxAgeDays: null }
];

const FALLBACK_POINTS_BY_INTERVAL = {
  '1D': 1,
  '1W': 5,
  '1M': 22,
  '1Y': 252,
  '3Y': 156,
  '5Y': 260,
  '10Y': 520,
  'ALL': null
};

export const filterChartDataByInterval = (points, intervalValue, nowMs = Date.now()) => {
  const selected = INTERVAL_OPTIONS.find((option) => option.value === intervalValue) || INTERVAL_OPTIONS[2];
  if (!selected.maxAgeDays) {
    return points;
  }

  const cutoff = nowMs - (selected.maxAgeDays * 24 * 60 * 60 * 1000);
  const filtered = points.filter((point) => {
    const parsed = new Date(point.timestamp).getTime();
    if (Number.isNaN(parsed)) return false;
    return parsed >= cutoff;
  });

  if (filtered.length > 0) {
    return filtered;
  }

  const fallbackCount = FALLBACK_POINTS_BY_INTERVAL[intervalValue] ?? null;
  if (!fallbackCount || points.length <= fallbackCount) {
    return points;
  }

  return points.slice(-fallbackCount);
};

const OhlcvTooltip = ({ active, payload }) => {
  if (!active || !payload || payload.length === 0) return null;

  const point = payload[0]?.payload;
  if (!point) return null;

  const isUp = Number(point.close) >= Number(point.open);

  const formatPrice = (value) => Number.isFinite(value) ? value.toFixed(2) : '—';
  const formatVolume = (value) => Number.isFinite(value) ? `${(value / 1000000).toFixed(1)}M` : '—';

  return (
    <div style={{
      background: '#1a2332',
      border: '1px solid #2a3a52',
      borderRadius: '8px',
      color: '#e6eef6',
      fontSize: '12px',
      padding: '10px 12px',
      minWidth: '170px'
    }}>
      <div style={{ color: '#e6eef6', fontWeight: 'bold', marginBottom: '8px' }}>{point.timestamp}</div>
      <div style={{ display: 'grid', gridTemplateColumns: 'auto auto', gap: '4px 10px' }}>
        <span style={{ color: '#9aa4b2' }}>Open</span><span>${formatPrice(point.open)}</span>
        <span style={{ color: '#9aa4b2' }}>High</span><span>${formatPrice(point.high)}</span>
        <span style={{ color: '#9aa4b2' }}>Low</span><span>${formatPrice(point.low)}</span>
        <span style={{ color: '#9aa4b2' }}>Close</span><span style={{ color: isUp ? '#00d19a' : '#ff5252', fontWeight: 'bold' }}>${formatPrice(point.close)}</span>
        <span style={{ color: '#9aa4b2' }}>Volume</span><span>{formatVolume(point.volume)}</span>
      </div>
    </div>
  );
};

const CandlestickSeries = ({ data, offset, priceDomain }) => {
  if (!data || data.length === 0) return null;
  if (!offset || !priceDomain || priceDomain.length !== 2) return null;

  const [minPrice, maxPrice] = priceDomain;
  const priceRange = maxPrice - minPrice;
  if (priceRange <= 0) return null;

  const step = offset.width / data.length;
  const candleWidth = Math.max(3, Math.min(10, step * 0.55));

  const toY = (value) => {
    const normalized = (value - minPrice) / priceRange;
    return offset.top + offset.height - (normalized * offset.height);
  };

  return (
    <g>
      {data.map((d, idx) => {
        const centerX = offset.left + (idx * step) + (step / 2);
        const x = centerX - candleWidth / 2;
        const yHigh = toY(d.high);
        const yLow = toY(d.low);
        const yOpen = toY(d.open);
        const yClose = toY(d.close);

        if ([yHigh, yLow, yOpen, yClose].some(v => Number.isNaN(v))) return null;

        const bodyTop = Math.min(yOpen, yClose);
        const bodyHeight = Math.max(1, Math.abs(yOpen - yClose));
        const isUp = d.close >= d.open;
        const color = isUp ? '#00d19a' : '#ff5252';

        return (
          <g key={`${d.timestamp}-${idx}`}>
            <line
              x1={centerX}
              x2={centerX}
              y1={yHigh}
              y2={yLow}
              stroke={color}
              strokeWidth={1}
            />
            <rect
              x={x}
              y={bodyTop}
              width={candleWidth}
              height={bodyHeight}
              fill={color}
            />
          </g>
        );
      })}
    </g>
  );
};

export default function StockDetail({ symbolOverride = null, planTier = 'FREE' }) {
  const { symbol } = useParams();
  const navigate = useNavigate();
  const activeSymbol = (symbolOverride || symbol || 'AAPL').toUpperCase();
  const embeddedMode = Boolean(symbolOverride);
  
  const [stock, setStock] = useState(null);
  const [timeInterval, setTimeInterval] = useState('1M');
  const [chartData, setChartData] = useState([]);
  const [baseSeries, setBaseSeries] = useState([]);
  const [baseSeriesApiInterval, setBaseSeriesApiInterval] = useState(null);
  const [loading, setLoading] = useState(false);
  const [ohlcData, setOhlcData] = useState(null);
  const [chartType, setChartType] = useState('mountain');

  const fetchLivePrice = async (sym) => {
    try {
      const response = await fetch(`/api/stocks/${sym}/price`);
      if (!response.ok) {
        return;
      }
      const data = await response.json();
      const livePrice = Number(data?.price);
      if (Number.isFinite(livePrice) && livePrice > 0) {
        setStock(prev => prev ? { ...prev, price: livePrice } : prev);
      }
    } catch (err) {
      console.error('Error fetching live price:', err);
    }
  };

  const applyIntervalData = (series, intervalValue) => {
    const intervalFiltered = filterChartDataByInterval(series, intervalValue);

    if (intervalFiltered.length > 0) {
      const latest = intervalFiltered[intervalFiltered.length - 1];
      setOhlcData({
        open: latest.open,
        high: Math.max(...intervalFiltered.map(d => d.high)),
        low: Math.min(...intervalFiltered.map(d => d.low)),
        close: latest.close
      });
    } else {
      setOhlcData(null);
    }

    setChartData(intervalFiltered);
  };

  const renderCandlestickLayer = (chartProps) => (
    <CandlestickSeries
      {...chartProps}
      data={chartData}
      priceDomain={priceDomain}
    />
  );

  const formatXAxisLabel = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) return timestamp;

    if (timeInterval === '1D' || timeInterval === '1W' || timeInterval === '1M') {
      return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
    }
    return date.toLocaleDateString(undefined, { month: 'short', year: '2-digit' });
  };

  const buildSyntheticVolume = (timestamp, index, close, open) => {
    const seedSource = `${timestamp}-${index}`;
    let hash = 0;
    for (let i = 0; i < seedSource.length; i++) {
      hash = ((hash << 5) - hash) + seedSource.charCodeAt(i);
      hash |= 0;
    }
    const movementFactor = Math.max(0.5, Math.abs(close - open));
    const base = 120000;
    const variability = Math.abs(hash % 900000);
    return Math.floor(base + variability + (movementFactor * 30000));
  };
  
  // Fetch historical data from backend with caching
  const fetchHistoricalData = async (sym, intervalValue) => {
    try {
      setLoading(true);

      const selected = INTERVAL_OPTIONS.find((option) => option.value === intervalValue) || INTERVAL_OPTIONS[2];
      const apiInterval = selected.apiInterval;
      
      // Check cache first
      const cacheKey = `stock_data_v2_${sym}_${apiInterval}`;
      const cachedData = localStorage.getItem(cacheKey);
      
      let result;
      if (cachedData) {
        console.log(`Using cached data for ${sym}`);
        const parsed = JSON.parse(cachedData);
        const cachedSeries = Array.isArray(parsed?.data) ? parsed.data : [];
        if (apiInterval === 'daily' && cachedSeries.length < 30) {
          localStorage.removeItem(cacheKey);
          const response = await fetch(`/api/stocks/${sym}/history?interval=${apiInterval}`);
          result = await response.json();
        } else {
          result = parsed;
        }
      } else {
        const response = await fetch(`/api/stocks/${sym}/history?interval=${apiInterval}`);
        result = await response.json();
        
        // Cache the result if it has data
        if (result.data && result.data.length > 0) {
          localStorage.setItem(cacheKey, JSON.stringify(result));
        }
      }
      
      // Format data for charts with volume
      const formatted = (result?.data || []).map((item, idx) => ({
        timestamp: item.timestamp,
        open: parseFloat(item.open),
        high: parseFloat(item.high),
        low: parseFloat(item.low),
        close: parseFloat(item.close),
        volume: buildSyntheticVolume(item.timestamp, idx, parseFloat(item.close), parseFloat(item.open))
      })).reverse(); // Reverse to show oldest first

      setBaseSeries(formatted);
      setBaseSeriesApiInterval(apiInterval);
      applyIntervalData(formatted, intervalValue);
    } catch (err) {
      console.error('Error fetching data:', err);
      setChartData([]);
      setBaseSeries([]);
      setBaseSeriesApiInterval(null);
      setOhlcData(null);
    } finally {
      setLoading(false);
    }
  };
  
  useEffect(() => {
    const tempStock = {
      symbol: activeSymbol,
      name: activeSymbol,
      price: 0,
      id: `stock-${activeSymbol}`
    };
    setStock(tempStock);
    setBaseSeries([]);
    setBaseSeriesApiInterval(null);

    fetchLivePrice(activeSymbol);
    const liveInterval = setInterval(() => fetchLivePrice(activeSymbol), 30000);
    return () => clearInterval(liveInterval);
  }, [activeSymbol]);

  useEffect(() => {
    return subscribeToQuoteStream([activeSymbol], (quotes) => {
      const quote = quotes.find((item) => String(item?.symbol || '').toUpperCase() === activeSymbol);
      if (!quote) return;

      const livePrice = Number(quote?.price);
      if (Number.isFinite(livePrice) && livePrice > 0) {
        setStock((prev) => (prev ? { ...prev, price: livePrice } : prev));
      }
    });
  }, [activeSymbol]);
  
  useEffect(() => {
    if (!stock) return;

    const selected = INTERVAL_OPTIONS.find((option) => option.value === timeInterval) || INTERVAL_OPTIONS[2];
    if (baseSeries.length > 0 && baseSeriesApiInterval === selected.apiInterval) {
      applyIntervalData(baseSeries, timeInterval);
      return;
    }

    fetchHistoricalData(stock.symbol, timeInterval);
  }, [stock, timeInterval, baseSeries, baseSeriesApiInterval]);
  
  if (!stock) return <div style={{ color: '#e6eef6', textAlign: 'center', padding: '40px' }}>Loading...</div>;

  const lowValues = chartData.map(d => d.low).filter(v => Number.isFinite(v));
  const highValues = chartData.map(d => d.high).filter(v => Number.isFinite(v));
  const volumeValues = chartData.map(d => d.volume).filter(v => Number.isFinite(v));

  const minPrice = lowValues.length ? Math.min(...lowValues) : 0;
  const maxPrice = highValues.length ? Math.max(...highValues) : 100;
  const pricePadding = (maxPrice - minPrice) * 0.08;
  const priceDomain = [Math.max(0, minPrice - pricePadding), maxPrice + pricePadding];
  const maxVolume = volumeValues.length ? Math.max(...volumeValues) : 1000000;
  
  return (
    <div style={{ padding: '20px', color: '#e6eef6', maxWidth: '1200px', margin: '0 auto' }}>
      <span style={{ display: 'none' }} data-testid="interval-state">{timeInterval}:{chartData.length}</span>
      {!embeddedMode && (
        <div style={{ marginBottom: '20px' }}>
          <button
            onClick={() => navigate('/')}
            style={{
              padding: '8px 16px',
              borderRadius: '6px',
              border: '1px solid #2a3a52',
              background: 'transparent',
              color: '#00d19a',
              cursor: 'pointer',
              fontWeight: 'bold'
            }}
          >
            ← Back to Watchlist
          </button>
        </div>
      )}

      {/* Stock Header with Price and Volume*/}
      <div style={{ marginBottom: '30px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '15px', marginBottom: '15px' }}>
            <img 
              src={getLogoUrl(stock.symbol)} 
              alt={stock.symbol}
              style={{
                width: '48px',
                height: '48px',
                borderRadius: '6px',
                objectFit: 'contain',
                background: '#1a2332',
                padding: '4px'
              }}
              onError={(e) => {
                const fallback = getInitialsBadge(stock.symbol)
                if (fallback) {
                  e.target.src = fallback
                } else {
                  e.target.style.display = 'none'
                }
              }}
            />
            <div>
              <h1 style={{ fontSize: '28px', margin: '0 0 5px 0', fontWeight: 'bold' }}>
                {stock.symbol}
              </h1>
              <p style={{ color: '#9aa4b2', margin: '0', fontSize: '14px' }}>{stock.name}</p>
            </div>
          </div>
          
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '15px', marginBottom: '15px' }}>
            <span style={{ fontSize: '48px', fontWeight: 'bold', color: '#e6eef6' }}>
              ${stock && stock.price > 0 ? stock.price.toFixed(2) : '—'}
            </span>
            <span style={{ color: '#00d19a', fontSize: '18px', fontWeight: 'bold' }}>
              +${stock && stock.price > 0 ? (stock.price * 0.05).toFixed(2) : '—'} (-1.94%)
            </span>
          </div>
          
          <div style={{ fontSize: '12px', color: '#9aa4b2' }}>
            Vol {chartData.length > 0 ? (chartData[chartData.length - 1].volume / 1000000).toFixed(2) : 0}M
          </div>
        </div>
        
        {/* OHLC Data Box */}
        <div style={{
          background: '#1a2332',
          border: '1px solid #2a3a52',
          borderRadius: '8px',
          padding: '15px 20px',
          fontSize: '13px',
          minWidth: '180px'
        }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px 20px' }}>
            <div>
              <div style={{ color: '#9aa4b2', marginBottom: '4px' }}>OPEN</div>
              <div style={{ color: '#e6eef6', fontWeight: 'bold' }}>
                ${ohlcData?.open.toFixed(2) || '—'}
              </div>
            </div>
            <div>
              <div style={{ color: '#9aa4b2', marginBottom: '4px' }}>CLOSE</div>
              <div style={{ color: '#e6eef6', fontWeight: 'bold' }}>
                ${ohlcData?.close.toFixed(2) || '—'}
              </div>
            </div>
            <div>
              <div style={{ color: '#9aa4b2', marginBottom: '4px' }}>HIGH</div>
              <div style={{ color: '#e6eef6', fontWeight: 'bold' }}>
                ${ohlcData?.high.toFixed(2) || '—'}
              </div>
            </div>
            <div>
              <div style={{ color: '#9aa4b2', marginBottom: '4px' }}>LOW</div>
              <div style={{ color: '#e6eef6', fontWeight: 'bold' }}>
                ${ohlcData?.low.toFixed(2) || '—'}
              </div>
            </div>
          </div>
        </div>
      </div>
      
      {/* Time Interval + Chart Type Controls */}
      <div style={{ marginBottom: '20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          {INTERVAL_OPTIONS.map(int => (
            <button
              key={int.value}
              onClick={() => setTimeInterval(int.value)}
              style={{
                padding: '8px 14px',
                borderRadius: '6px',
                border: timeInterval === int.value ? '2px solid #00d19a' : '1px solid #2a3a52',
                background: timeInterval === int.value ? 'rgba(0, 209, 154, 0.1)' : 'transparent',
                color: timeInterval === int.value ? '#00d19a' : '#9aa4b2',
                cursor: 'pointer',
                fontWeight: 'bold',
                fontSize: '12px'
              }}
            >
              {int.label}
            </button>
          ))}
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            onClick={() => setChartType('mountain')}
            style={{
              padding: '8px 12px',
              borderRadius: '6px',
              border: chartType === 'mountain' ? '2px solid #00d19a' : '1px solid #2a3a52',
              background: chartType === 'mountain' ? 'rgba(0, 209, 154, 0.1)' : 'transparent',
              color: chartType === 'mountain' ? '#00d19a' : '#9aa4b2',
              cursor: 'pointer',
              fontWeight: 'bold',
              fontSize: '12px'
            }}
          >
            Mountain
          </button>

          <FeatureGate
            currentTier={planTier}
            requiredTier="PRO"
            fallback={
              <button
                disabled
                title="Upgrade to Pro to use Candlestick"
                style={{
                  padding: '8px 12px',
                  borderRadius: '6px',
                  border: '1px solid #2a3a52',
                  background: 'transparent',
                  color: '#67768a',
                  cursor: 'not-allowed',
                  fontWeight: 'bold',
                  fontSize: '12px'
                }}
              >
                Candlestick 🔒
              </button>
            }
          >
            <button
              onClick={() => setChartType('candlestick')}
              style={{
                padding: '8px 12px',
                borderRadius: '6px',
                border: chartType === 'candlestick' ? '2px solid #00d19a' : '1px solid #2a3a52',
                background: chartType === 'candlestick' ? 'rgba(0, 209, 154, 0.1)' : 'transparent',
                color: chartType === 'candlestick' ? '#00d19a' : '#9aa4b2',
                cursor: 'pointer',
                fontWeight: 'bold',
                fontSize: '12px'
              }}
            >
              Candlestick
            </button>
          </FeatureGate>
        </div>
      </div>
      
      {/* Chart with Volume */}
      <div style={{
        background: '#0b1020',
        border: '1px solid #2a3a52',
        borderRadius: '12px',
        padding: '20px',
        height: '500px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}>
        {loading ? (
          <div style={{ color: '#9aa4b2' }}>Loading chart data...</div>
        ) : chartData && chartData.length > 0 ? (
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={chartData} margin={{ top: 20, right: 30, left: 0, bottom: 60 }}>
              <defs>
                <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#00d19a" stopOpacity={0.3}/>
                  <stop offset="95%" stopColor="#00d19a" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#2a3a52" />
              <XAxis 
                dataKey="timestamp"
                stroke="#9aa4b2" 
                style={{ fontSize: '12px' }}
                interval={Math.floor(chartData.length / 10)}
                tickFormatter={formatXAxisLabel}
                tickLine={false}
                axisLine={{ stroke: '#2a3a52' }}
              />
              <YAxis 
                stroke="#9aa4b2" 
                yAxisId="left"
                style={{ fontSize: '12px' }}
                orientation="right"
                domain={priceDomain}
                tickFormatter={(value) => `${Number(value).toFixed(2)}`}
                tickLine={false}
                axisLine={{ stroke: '#2a3a52' }}
                width={80}
              />
              <YAxis 
                hide
                yAxisId="right" 
                domain={[0, maxVolume * 4]}
              />
              <Tooltip
                content={<OhlcvTooltip />}
              />
              
              {/* Volume bars */}
              <Bar 
                yAxisId="right"
                dataKey="volume" 
                opacity={0.65}
                isAnimationActive={false}
              >
                {chartData.map((entry, idx) => (
                  <Cell
                    key={`vol-${entry.timestamp}-${idx}`}
                    fill={entry.close >= entry.open ? '#00d19a' : '#ff5252'}
                    fillOpacity={0.35}
                  />
                ))}
              </Bar>

              {/* Keep left axis domain stable for candlesticks */}
              <Area
                yAxisId="left"
                type="monotone"
                dataKey="close"
                stroke="transparent"
                fill="transparent"
                isAnimationActive={false}
                activeDot={false}
              />
              
              {chartType === 'mountain' ? (
                <Area
                  yAxisId="left"
                  type="monotone"
                  dataKey="close"
                  stroke="#00d19a"
                  fillOpacity={1}
                  fill="url(#colorPrice)"
                  isAnimationActive={false}
                />
              ) : (
                <Customized component={renderCandlestickLayer} />
              )}
            </ComposedChart>
          </ResponsiveContainer>
        ) : (
          <div style={{ color: '#9aa4b2', textAlign: 'center', padding: '40px' }}>
            <p style={{ margin: '0 0 10px 0' }}>No data available for this interval.</p>
            <p style={{ margin: '0', fontSize: '12px', color: '#7a8a9a' }}>
              This may be due to API rate limiting. The free tier allows 25 requests per day.<br/>
              Try again later or use existing stocks (AAPL, MSFT, TSLA).
            </p>
          </div>
        )}
      </div>

      <AlertsPanel symbol={stock.symbol} />
      
    </div>
  );
}
