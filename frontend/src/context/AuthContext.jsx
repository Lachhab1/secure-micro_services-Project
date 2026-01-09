import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import keycloak from '../keycloak';

const AuthContext = createContext(null);

/**
 * Provider d'authentification Keycloak.
 */
export function AuthProvider({ children }) {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(null);
    const [roles, setRoles] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const initKeycloak = async () => {
            try {
                const authenticated = await keycloak.init({
                    onLoad: 'login-required',
                    checkLoginIframe: false,
                    pkceMethod: 'S256'
                });

                if (authenticated) {
                    setIsAuthenticated(true);
                    setToken(keycloak.token);

                    // Extraire les informations utilisateur
                    const userProfile = {
                        id: keycloak.subject,
                        username: keycloak.tokenParsed?.preferred_username,
                        email: keycloak.tokenParsed?.email,
                        firstName: keycloak.tokenParsed?.given_name,
                        lastName: keycloak.tokenParsed?.family_name
                    };
                    setUser(userProfile);

                    // Extraire les rôles
                    const userRoles = keycloak.tokenParsed?.realm_access?.roles || [];
                    setRoles(userRoles);

                    // Rafraîchir le token automatiquement
                    setInterval(() => {
                        keycloak.updateToken(60)
                            .then((refreshed) => {
                                if (refreshed) {
                                    setToken(keycloak.token);
                                }
                            })
                            .catch(() => {
                                console.error('Échec du rafraîchissement du token');
                                logout();
                            });
                    }, 30000);
                }
            } catch (err) {
                console.error('Erreur d\'initialisation Keycloak:', err);
                setError('Erreur de connexion au serveur d\'authentification');
            } finally {
                setIsLoading(false);
            }
        };

        initKeycloak();
    }, []);

    const logout = useCallback(() => {
        keycloak.logout({ redirectUri: window.location.origin });
    }, []);

    const hasRole = useCallback((role) => {
        return roles.includes(role);
    }, [roles]);

    const isAdmin = useCallback(() => {
        return hasRole('ADMIN');
    }, [hasRole]);

    const isClient = useCallback(() => {
        return hasRole('CLIENT');
    }, [hasRole]);

    const value = {
        isAuthenticated,
        user,
        token,
        roles,
        isLoading,
        error,
        logout,
        hasRole,
        isAdmin,
        isClient
    };

    if (isLoading) {
        return (
            <div className="loading">
                <div className="spinner"></div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="app-container">
                <div className="alert alert-error">
                    <h2>Erreur d'authentification</h2>
                    <p>{error}</p>
                    <button onClick={() => window.location.reload()} className="btn btn-primary">
                        Réessayer
                    </button>
                </div>
            </div>
        );
    }

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
