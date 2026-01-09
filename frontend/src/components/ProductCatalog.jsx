import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { productApi } from '../services/api';
import { useAuth } from '../context/AuthContext';

/**
 * Catalogue de produits avec actions CRUD pour les admins.
 */
function ProductCatalog() {
    const { isAdmin } = useAuth();
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [searchTerm, setSearchTerm] = useState('');

    const { data: products, isLoading, error } = useQuery({
        queryKey: ['products'],
        queryFn: async () => {
            const response = await productApi.getAll();
            return response.data;
        }
    });

    const deleteMutation = useMutation({
        mutationFn: productApi.delete,
        onSuccess: () => {
            queryClient.invalidateQueries(['products']);
        }
    });

    const handleDelete = (id, name) => {
        if (window.confirm(`Êtes-vous sûr de vouloir supprimer "${name}" ?`)) {
            deleteMutation.mutate(id);
        }
    };

    const getStockStatus = (quantity) => {
        if (quantity === 0) return { class: 'out-of-stock', text: 'Rupture' };
        if (quantity <= 10) return { class: 'low-stock', text: 'Stock faible' };
        return { class: 'in-stock', text: 'En stock' };
    };

    const filteredProducts = products?.filter(product =>
        product.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        product.description?.toLowerCase().includes(searchTerm.toLowerCase())
    );

    if (isLoading) {
        return <div className="loading"><div className="spinner"></div></div>;
    }

    if (error) {
        return (
            <div className="alert alert-error">
                <strong>Erreur:</strong> {error.response?.data?.message || 'Impossible de charger les produits'}
            </div>
        );
    }

    return (
        <div>
            <div className="card" style={{ marginBottom: '1.5rem' }}>
                <div className="card-header">
                    <h2 className="card-title">📦 Catalogue Produits</h2>
                    <input
                        type="text"
                        placeholder="🔍 Rechercher un produit..."
                        className="form-input"
                        style={{ maxWidth: '300px' }}
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                </div>
                <p>
                    {filteredProducts?.length || 0} produit(s) trouvé(s)
                </p>
            </div>

            <div className="grid grid-3">
                {filteredProducts?.map((product) => {
                    const stockStatus = getStockStatus(product.stockQuantity);

                    return (
                        <div key={product.id} className="card product-card">
                            <h3 style={{ marginBottom: '0.5rem' }}>{product.name}</h3>
                            <p style={{ color: 'var(--gray-600)', marginBottom: '1rem', minHeight: '3rem' }}>
                                {product.description || 'Pas de description'}
                            </p>

                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                                <span className="product-price">{product.price?.toFixed(2)} €</span>
                                <span className={`product-stock ${stockStatus.class}`}>
                                    {stockStatus.text} ({product.stockQuantity})
                                </span>
                            </div>

                            {isAdmin() && (
                                <div style={{ display: 'flex', gap: '0.5rem' }}>
                                    <button
                                        className="btn btn-secondary btn-sm"
                                        onClick={() => navigate(`/products/${product.id}/edit`)}
                                    >
                                        ✏️ Modifier
                                    </button>
                                    <button
                                        className="btn btn-danger btn-sm"
                                        onClick={() => handleDelete(product.id, product.name)}
                                        disabled={deleteMutation.isPending}
                                    >
                                        🗑️ Supprimer
                                    </button>
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>

            {filteredProducts?.length === 0 && (
                <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
                    <p style={{ color: 'var(--gray-500)' }}>Aucun produit trouvé</p>
                </div>
            )}
        </div>
    );
}

export default ProductCatalog;
