/**
 * Map stock symbols to company names for logo lookup
 */
const logoMap = {
  'AAPL': 'apple.com',
  'MSFT': 'microsoft.com',
  'GOOGL': 'google.com',
  'GOOG': 'google.com',
  'AMZN': 'amazon.com',
  'NVDA': 'nvidia.com',
  'META': 'meta.com',
  'TSLA': 'tesla.com',
  'ADBE': 'adobe.com',
  'NFLX': 'netflix.com',
  'INTC': 'intel.com',
  'AMD': 'amd.com',
  'CRM': 'salesforce.com',
  'ORCL': 'oracle.com',
  'IBM': 'ibm.com',
  'SAP': 'sap.com',
  'MU': 'micron.com',
  'QCOM': 'qualcomm.com',
  'AVGO': 'broadcom.com',
  'ASML': 'asml.com',
  'TSM': 'tsmc.com.tw',
  'JPM': 'jpmorganchase.com',
  'BAC': 'bankofamerica.com',
  'WFC': 'wellsfargo.com',
  'GS': 'goldmansachs.com',
  'MS': 'morganstanley.com',
  'JNJ': 'jnj.com',
  'UNH': 'unitedhealthgroup.com',
  'PFE': 'pfizer.com',
  'MRNA': 'modernatx.com',
  'ABT': 'abbott.com',
  'WMT': 'walmart.com',
  'COST': 'costco.com',
  'MCD': 'mcdonalds.com',
  'NKE': 'nike.com',
  'HD': 'homedepot.com',
  'XOM': 'exxonmobil.com',
  'CVX': 'chevron.com',
  'NEE': 'nexteraenergy.com',
  'BRK.B': 'berkshirehathaway.com',
  'LLY': 'lilly.com',
  'V': 'visa.com',
  'MA': 'mastercard.com',
  'PLTR': 'palantir.com',
  'RIVN': 'rivianmotors.com',
  'IREN': 'irenerating.com',
}

/**
 * Get logo URL for a stock symbol using multiple reliable sources
 * @param {string} symbol - Stock symbol (e.g., 'AAPL')
 * @returns {string} Logo URL from reliable service
 */
export function getLogoUrl(symbol) {
  if (!symbol) return null;
  
  const upper = symbol.toUpperCase();
  const domain = logoMap[upper];
  
  if (!domain) {
    return getInitialsBadge(symbol);
  }
  
  // Use DuckDuckGo's icon service (most reliable)
  // Format: https://icons.duckduckgo.com/ip3/{domain}.ico
  // This returns high-quality corporate logos
  return `https://icons.duckduckgo.com/ip3/${domain}.ico`;
}

/**
 * Get SVG badge with stock symbol initials as fallback
 * @param {string} symbol - Stock symbol
 * @returns {string} Data URI for SVG badge
 */
export function getInitialsBadge(symbol) {
  if (!symbol) return null;
  
  const initials = symbol.substring(0, 3).toUpperCase();
  // Create an SVG badge with initials
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48">
    <defs>
      <linearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" style="stop-color:#00d19a;stop-opacity:1" />
        <stop offset="100%" style="stop-color:#0099ff;stop-opacity:1" />
      </linearGradient>
    </defs>
    <rect width="48" height="48" rx="4" fill="url(#grad)"/>
    <text x="24" y="26" text-anchor="middle" font-size="18" font-weight="bold" fill="white" font-family="Arial, sans-serif">${initials}</text>
  </svg>`;
  
  const encoded = btoa(svg).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  return `data:image/svg+xml;base64,${btoa(svg)}`;
}

/**
 * Get just the initials for display
 * @param {string} symbol - Stock symbol
 * @returns {string} 1-3 letter initials
 */
export function getSymbolInitials(symbol) {
  return (symbol || '').substring(0, 3).toUpperCase();
}

export default { getLogoUrl, getInitialsBadge, getSymbolInitials }
