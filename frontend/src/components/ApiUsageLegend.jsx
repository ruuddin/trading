import React, { useEffect, useState } from 'react';

/**
 * Fixed footer legend displaying API usage metrics
 * Shows requests completed and rate limit status for each provider
 */
const ApiUsageLegend = () => {
  const [metrics, setMetrics] = useState({});
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchMetrics = async () => {
      try {
        const response = await fetch('/api/metrics/summary');
        const data = await response.json();
        setSummary(data);
        setMetrics(data.providers || {});
        setLoading(false);
      } catch (error) {
        console.error('Error fetching API metrics:', error);
        setLoading(false);
      }
    };

    // Initial fetch
    fetchMetrics();

    // Poll every 10 seconds for updates
    const interval = setInterval(fetchMetrics, 10000);
    return () => clearInterval(interval);
  }, []);

  const getProviderColor = (rateLimited, usagePercent) => {
    if (rateLimited) return '#ef4444';
    if (usagePercent > 90) return '#f97316';
    if (usagePercent > 70) return '#eab308';
    return '#22c55e';
  };

  const providerEntries = Object.entries(metrics);

  const footerStyle = {
    position: 'fixed',
    bottom: 0,
    left: 0,
    right: 0,
    zIndex: 60,
    background: '#0f1419',
    borderTop: '1px solid #2a3a52',
    padding: '10px 16px',
    color: '#e6eef6',
    fontSize: '12px'
  };

  const rowStyle = {
    display: 'flex',
    gap: '10px',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexWrap: 'wrap'
  };

  if (loading) {
    return (
      <div style={footerStyle}>
        API usage: loading...
      </div>
    );
  }

  return (
    <div style={footerStyle}>
      <div style={rowStyle}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#9aa4b2' }}>
          <strong style={{ color: '#e6eef6' }}>API Usage</strong>
          <span>
            {summary?.totalRequests || 0}/{summary?.totalDailyLimit || 0} requests
          </span>
          <span>({summary?.averageUsagePercent || '0%'} avg)</span>
        </div>

        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          {providerEntries.map(([provider, providerMetrics]) => {
            const usagePercent = providerMetrics?.dailyUsagePercent || 0;
            const rateLimited = providerMetrics?.rateLimited;

            return (
              <div
                key={provider}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '4px 8px',
                  borderRadius: '999px',
                  background: '#1a2332',
                  border: '1px solid #2a3a52'
                }}
              >
                <span
                  style={{
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    background: getProviderColor(rateLimited, usagePercent)
                  }}
                />
                <span style={{ color: '#9aa4b2' }}>{provider}</span>
                <span style={{ color: '#e6eef6' }}>
                  {providerMetrics?.dailyRequestCount || 0}/{providerMetrics?.dailyLimit || 0}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default ApiUsageLegend;
