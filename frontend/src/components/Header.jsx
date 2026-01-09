import { useAuth } from '../context/AuthContext';

/**
 * En-tête de l'application avec informations utilisateur.
 */
function Header() {
    const { user, roles, logout, isAdmin } = useAuth();

    return (
        <header className="header">
            <h1>
                🛒 Secure Microservices
            </h1>

            <div className="header-user">
                <div className="user-badge">
                    👤 {user?.firstName || user?.username}
                </div>
                <span className={`role-badge ${isAdmin() ? 'admin' : ''}`}>
                    {isAdmin() ? 'ADMIN' : 'CLIENT'}
                </span>
                <button onClick={logout} className="btn btn-secondary btn-sm">
                    Déconnexion
                </button>
            </div>
        </header>
    );
}

export default Header;
