import { useRealtimeFeed } from '../hooks/useRealtimeFeed';

export default function RealtimeFeed() {
  const { liveEvents, isConnected } = useRealtimeFeed();

  const getAlertColor = (freq: number) => {
    if (freq >= 8.0) return '#d32f2f'; // Red
    if (freq >= 3.0) return '#f57c00'; // Orange
    return '#fbc02d'; // Yellow
  };

  return (
    <div style={{ 
      width: '350px', 
      height: 'calc(100vh - 65px)', 
      overflowY: 'auto', 
      padding: '15px',
      // We removed the background color and the border-left!
      backgroundColor: 'transparent'
    }}>
      {/* We also removed the border-bottom under the title */}
      <h3 style={{ color: '#ffffff', marginTop: 0, paddingBottom: '10px' }}>
        Live Alerts {isConnected ? '🟢' : '🔴'}
      </h3>
      
      <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
        {liveEvents.length === 0 ? (
          <p style={{ color: '#888', fontStyle: 'italic' }}>Waiting for seismic events...</p>
        ) : (
          liveEvents.map((event, index) => (
            <div key={index} style={{ 
              backgroundColor: getAlertColor(event.frequencyHz), 
              padding: '15px', 
              borderRadius: '6px', 
              color: '#000000',
              boxShadow: '0 4px 6px rgba(0,0,0,0.3)'
            }}>
              <div style={{ fontWeight: '900', marginBottom: '8px', borderBottom: '1px solid rgba(0,0,0,0.2)', paddingBottom: '4px' }}>
                EVENT: {event.eventType.toUpperCase()}
              </div>
              <div style={{ fontSize: '0.9rem', lineHeight: '1.4' }}>
                <strong>EVENT ID:</strong> {event.eventId}<br/>
                <strong>TIME:</strong> {new Date(event.startsAt).toLocaleTimeString()}<br/>
                <strong>FREQ:</strong> {event.frequencyHz.toFixed(1)} Hz<br/>
                <strong>AMPLITUDE:</strong> {event.amplitude.toFixed(1)} mm/s
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}