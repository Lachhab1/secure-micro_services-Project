import axios from 'axios';
import keycloak from '../keycloak';

/**
 * Instance Axios configurée avec intercepteur JWT.
 */
const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
});

// Intercepteur pour ajouter le token JWT à chaque requête
api.interceptors.request.use(
    (config) => {
        if (keycloak.token) {
            config.headers.Authorization = `Bearer ${keycloak.token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Intercepteur pour gérer les erreurs
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // Gérer l'expiration du token (401)
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                await keycloak.updateToken(5);
                originalRequest.headers.Authorization = `Bearer ${keycloak.token}`;
                return api(originalRequest);
            } catch (refreshError) {
                // Rediriger vers la page de login
                keycloak.logout();
                return Promise.reject(refreshError);
            }
        }

        // Gérer les erreurs 403 (Forbidden)
        if (error.response?.status === 403) {
            console.error('Accès refusé:', error.response.data);
        }

        return Promise.reject(error);
    }
);

// API Products
export const productApi = {
    getAll: () => api.get('/api/products'),
    getById: (id) => api.get(`/api/products/${id}`),
    create: (product) => api.post('/api/products', product),
    update: (id, product) => api.put(`/api/products/${id}`, product),
    delete: (id) => api.delete(`/api/products/${id}`),
    search: (name) => api.get('/api/products/search', { params: { name } })
};

// API Orders
export const orderApi = {
    getAll: () => api.get('/api/orders'),
    getMyOrders: () => api.get('/api/orders/my'),
    getById: (id) => api.get(`/api/orders/${id}`),
    create: (order) => api.post('/api/orders', order),
    updateStatus: (id, status) => api.patch(`/api/orders/${id}/status`, { status }),
    cancel: (id) => api.post(`/api/orders/${id}/cancel`)
};

export default api;
