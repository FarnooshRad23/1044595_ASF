import { useState, useEffect } from 'react';
import type { ActiveSensorEvent } from '../types/api'; 

export function useRealtimeFeed() {
  // This state holds our list of events. It uses our blueprint to know exactly what it looks like!
  const [liveEvents, setLiveEvents] = useState<ActiveSensorEvent[]>([]);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    // Note: You may need to adjust this URL to match the exact WebSocket route your Gateway uses
    const ws = new WebSocket('ws://localhost:8080/ws/events'); 

    ws.onopen = () => {
      console.log('Connected to Aegis live event stream');
      setIsConnected(true);
    };

    ws.onmessage = (event) => {
      try {
        // When a message arrives, we read it using our ActiveSensorEvent blueprint
        const newEvent: ActiveSensorEvent = JSON.parse(event.data);
        
        // We add the new event to the very top of the list. 
        // We slice it to keep only the latest 50 so the browser's memory doesn't crash!
        setLiveEvents((prevEvents) => [newEvent, ...prevEvents].slice(0, 50));
      } catch (error) {
        console.error('Failed to parse incoming WS message:', error);
      }
    };

    ws.onclose = () => {
      console.log('Disconnected from live event stream');
      setIsConnected(false);
    };

    // This cleanup function runs if the analyst closes the dashboard
    return () => {
      ws.close();
    };
  }, []); // The empty array means this connection only opens once when the dashboard first loads

  return { liveEvents, isConnected };
}