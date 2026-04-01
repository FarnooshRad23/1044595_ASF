import axios from 'axios';

// This creates a reusable connection to your Java backend
// Base URL is 8080 based on your group's DOCKER_CONTRACT.md
const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

export default apiClient;