import Keycloak from 'keycloak-js';

/**
 * Configuration Keycloak pour l'application React.
 */
const keycloakConfig = {
    url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
    realm: import.meta.env.VITE_KEYCLOAK_REALM || 'secure-microservices',
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'frontend-app'
};

const keycloak = new Keycloak(keycloakConfig);

export default keycloak;
