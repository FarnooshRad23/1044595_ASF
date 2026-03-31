import { useState, useEffect } from 'react';

export default function SystemHealth() {
  const [time, setTime] = useState<string>('');

  useEffect(() => {
    const timer = setInterval(() => {
      const utcTime = new Date().toISOString().substring(11, 19);
      setTime(utcTime);
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '15px 20px',
      // We removed the background color and the border-bottom!
      backgroundColor: 'transparent', 
    }}>
      <h2 style={{ margin: 0, fontSize: '1.5rem', color: '#ffffff' }}>
        Neutral Zone Command Center - LIVE
      </h2>
      
      <div style={{ fontSize: '1.1rem', display: 'flex', alignItems: 'center', gap: '15px', color: '#cccccc' }}>
        <span style={{ color: '#4caf50', fontWeight: 'bold' }}>
          ● Status: System Nominal
        </span>
        <span>|</span>
        <span style={{ fontWeight: 'bold', color: '#ffffff' }}>
          UTC Time: {time}
        </span>
      </div>
    </div>
  );
}