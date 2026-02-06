import React, { useEffect } from 'react';

export default function AdBanner({ slot = 'default' }) {
  useEffect(() => {
    // Load Google AdSense script if not already loaded
    if (window.adsbygoogle && window.adsbygoogle.length > 0) {
      window.adsbygoogle.push({});
    }
  }, [slot]);

  return (
    <div className="ad-container my-4 px-4">
      {/* Google AdSense - Replace with your publisher ID */}
      <ins
        className="adsbygoogle"
        style={{
          display: 'block',
          minHeight: '100px',
          backgroundColor: '#1a1f2e',
          borderRadius: '0.5rem',
          border: '1px solid #2d3748',
        }}
        data-ad-client="ca-pub-xxxxxxxxxxxxxxxx"
        data-ad-slot={slot}
        data-ad-format="horizontal"
        data-full-width-responsive="true"
      ></ins>
      
      {/* Fallback message if no ad loads */}
      <div className="text-center text-gray-500 text-sm py-4">
        <p>Advertisement</p>
      </div>
    </div>
  );
}
