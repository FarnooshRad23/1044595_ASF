import { useRealtimeFeed } from '../hooks/useRealtimeFeed';

export default function RealtimeFeed() {
  const { liveEvents, isConnected } = useRealtimeFeed();

  // Helper to get the exact colors and titles based on your image rules
  const getAlertConfig = (freq: number) => {
    if (freq >= 8.0) {
      return { 
        color: '#ff3333', // Neon Red
        border: '1px solid #ff3333',
        glow: '0 0 12px rgba(255, 51, 51, 0.6)',
        title: 'CRITICAL: NUCLEAR-LIKE EVENT DETECTED',
        rule: 'f ≥ 8.0 Hz'
      };
    }
    if (freq >= 3.0) {
      return { 
        color: '#ff9900', // Neon Orange
        border: '1px solid #ff9900',
        glow: '0 0 12px rgba(255, 153, 0, 0.6)',
        title: 'WARNING: CONVENTIONAL EXPLOSION',
        rule: '3.0 ≤ f < 8.0 Hz'
      };
    }
    return { 
      color: '#ffff33', // Neon Yellow
      border: '1px solid #ffff33',
      glow: '0 0 12px rgba(255, 255, 51, 0.6)',
      title: 'ADVISORY: EARTHQUAKE',
      rule: '0.5 ≤ f < 3.0 Hz'
    };
  };

  return (
    <div style={{ 
      width: '450px', // Made it a bit wider to fit the text from your image
      height: 'calc(100vh - 65px)', 
      overflowY: 'auto', 
      padding: '20px',
      backgroundColor: '#111111' // Darker background to make the neon pop
    }}>
      <h3 style={{ color: '#ffffff', marginTop: 0, paddingBottom: '15px', fontSize: '1.2rem', fontWeight: 'normal' }}>
        Alert Box {isConnected ? '🟢' : '🔴'}
      </h3>
      
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        {liveEvents.length === 0 ? (
          <p style={{ color: '#888', fontStyle: 'italic' }}>Waiting for seismic events...</p>
        ) : (
          liveEvents.map((event, index) => {
            const config = getAlertConfig(event.frequencyHz);
            
            return (
              <div key={index} style={{ 
                backgroundColor: '#0a0a0a', // Very dark inner background
                padding: '12px 16px', 
                borderRadius: '4px', 
                color: '#ffffff',
                border: config.border,
                boxShadow: config.glow,
                fontFamily: 'monospace', // Gives it that terminal/data look
                position: 'relative'
              }}>
                
                {/* Header Row */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                  <div style={{ color: config.color, fontWeight: 'bold', fontSize: '1rem', textShadow: `0 0 5px ${config.color}` }}>
                    {config.title} <span style={{ color: '#ccc', fontWeight: 'normal', fontSize: '0.85rem', textShadow: 'none' }}>(Event Type based on {config.rule})</span>
                  </div>
                </div>

                {/* Data Rows */}
                <div style={{ fontSize: '0.95rem', lineHeight: '1.5', display: 'grid', gridTemplateColumns: '130px 1fr', gap: '4px' }}>
                  <span style={{ color: '#aaa' }}>SENSOR:</span> 
                  <span>{event.eventId}</span> {/* Assuming you map sensor details here later */}
                  
                  <span style={{ color: '#aaa' }}>TIMESTAMP:</span> 
                  <span>{new Date(event.startsAt).toISOString().replace('T', ' ').substring(0, 19)}Z</span>
                  
                  <span style={{ color: '#aaa' }}>DOMINANT FREQ:</span> 
                  <span>{event.frequencyHz.toFixed(1)} Hz</span>
                  
                  <span style={{ color: '#aaa' }}>AMPLITUDE:</span> 
                  <span>{event.amplitude.toFixed(1)} mm/s</span>
                </div>

                {/* Top Right Buttons */}
                <div style={{ position: 'absolute', top: '10px', right: '10px', display: 'flex', gap: '6px' }}>
                  <button style={{ backgroundColor: '#1a365d', color: '#63b3ed', border: '1px solid #2b6cb0', padding: '2px 8px', borderRadius: '3px', cursor: 'pointer', fontSize: '0.75rem' }}>
                    DETAILS
                  </button>
                  <button style={{ backgroundColor: 'transparent', color: '#888', border: '1px solid #444', padding: '2px 6px', borderRadius: '3px', cursor: 'pointer', fontSize: '0.75rem' }}>
                    X
                  </button>
                </div>

              </div>
            );
          })
        )}
      </div>
    </div>
  );
}