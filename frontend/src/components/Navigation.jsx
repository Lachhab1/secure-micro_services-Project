import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Navigation avec onglets adaptée au rôle.
 */
function Navigation() {
    const navigate = useNavigate();
    const location = useLocation();
    const { isAdmin } = useAuth();

    const isActive = (path) => location.pathname.startsWith(path);

    return (
        <nav className="nav-tabs">
            <button
                className={`nav-tab ${isActive('/products') ? 'active' : ''}`}
                onClick={() => navigate('/products')}
            >
                📦 Catalogue Produits
            </button>

            <button
                className={`nav-tab ${isActive('/orders') ? 'active' : ''}`}
                onClick={() => navigate('/orders')}
            >
                📋 {isAdmin() ? 'Toutes les Commandes' : 'Mes Commandes'}
            </button>

            <button
                className={`nav-tab ${location.pathname === '/orders/new' ? 'active' : ''}`}
                onClick={() => navigate('/orders/new')}
            >
                ➕ Nouvelle Commande
            </button>

            {isAdmin() && (
                <button
                    className={`nav-tab ${location.pathname === '/products/new' ? 'active' : ''}`}
                    onClick={() => navigate('/products/new')}
                >
                    ➕ Ajouter Produit
                </button>
            )}
        </nav>
    );
}

export default Navigation;
