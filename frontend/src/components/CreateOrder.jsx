import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productApi, orderApi } from '../services/api';

/**
 * Page de création de commande pour les clients.
 */
function CreateOrder() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [cart, setCart] = useState([]);
    const [error, setError] = useState(null);

    const { data: products, isLoading: loadingProducts } = useQuery({
        queryKey: ['products'],
        queryFn: async () => {
            const response = await productApi.getAll();
            return response.data;
        }
    });

    const createOrderMutation = useMutation({
        mutationFn: orderApi.create,
        onSuccess: () => {
            queryClient.invalidateQueries(['orders']);
            queryClient.invalidateQueries(['products']);
            navigate('/orders');
        },
        onError: (err) => {
            setError(err.response?.data?.message || 'Erreur lors de la création de la commande');
        }
    });

    const addToCart = (product) => {
        const existingItem = cart.find(item => item.productId === product.id);

        if (existingItem) {
            if (existingItem.quantity >= product.stockQuantity) {
                setError(`Stock insuffisant pour ${product.name}`);
                return;
            }
            setCart(cart.map(item =>
                item.productId === product.id
                    ? { ...item, quantity: item.quantity + 1 }
                    : item
            ));
        } else {
            if (product.stockQuantity < 1) {
                setError(`${product.name} est en rupture de stock`);
                return;
            }
            setCart([...cart, {
                productId: product.id,
                productName: product.name,
                price: product.price,
                quantity: 1,
                maxQuantity: product.stockQuantity
            }]);
        }
        setError(null);
    };

    const updateQuantity = (productId, quantity) => {
        if (quantity < 1) {
            removeFromCart(productId);
            return;
        }

        const item = cart.find(i => i.productId === productId);
        if (quantity > item.maxQuantity) {
            setError(`Quantité maximale disponible: ${item.maxQuantity}`);
            return;
        }

        setCart(cart.map(item =>
            item.productId === productId ? { ...item, quantity } : item
        ));
        setError(null);
    };

    const removeFromCart = (productId) => {
        setCart(cart.filter(item => item.productId !== productId));
    };

    const getTotalPrice = () => {
        return cart.reduce((total, item) => total + (item.price * item.quantity), 0);
    };

    const handleSubmit = () => {
        if (cart.length === 0) {
            setError('Veuillez ajouter au moins un produit à votre commande');
            return;
        }

        const orderData = {
            items: cart.map(item => ({
                productId: item.productId,
                quantity: item.quantity
            }))
        };

        createOrderMutation.mutate(orderData);
    };

    const availableProducts = products?.filter(p => p.stockQuantity > 0) || [];

    if (loadingProducts) {
        return <div className="loading"><div className="spinner"></div></div>;
    }

    return (
        <div className="grid grid-2">
            {/* Produits disponibles */}
            <div className="card">
                <div className="card-header">
                    <h2 className="card-title">📦 Produits Disponibles</h2>
                </div>

                {availableProducts.length === 0 ? (
                    <p style={{ color: 'var(--gray-500)' }}>Aucun produit disponible</p>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        {availableProducts.map((product) => (
                            <div key={product.id} style={{
                                display: 'flex',
                                justifyContent: 'space-between',
                                alignItems: 'center',
                                padding: '1rem',
                                background: 'var(--gray-50)',
                                borderRadius: 'var(--radius-lg)'
                            }}>
                                <div>
                                    <strong>{product.name}</strong>
                                    <div style={{ fontSize: '0.875rem', color: 'var(--gray-600)' }}>
                                        {product.price?.toFixed(2)} € • Stock: {product.stockQuantity}
                                    </div>
                                </div>
                                <button
                                    className="btn btn-primary btn-sm"
                                    onClick={() => addToCart(product)}
                                >
                                    + Ajouter
                                </button>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Panier */}
            <div className="card">
                <div className="card-header">
                    <h2 className="card-title">🛒 Votre Panier</h2>
                    <span style={{ color: 'var(--gray-600)' }}>{cart.length} article(s)</span>
                </div>

                {error && (
                    <div className="alert alert-error" style={{ marginBottom: '1rem' }}>
                        {error}
                    </div>
                )}

                {cart.length === 0 ? (
                    <p style={{ color: 'var(--gray-500)', textAlign: 'center', padding: '2rem' }}>
                        Votre panier est vide
                    </p>
                ) : (
                    <>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginBottom: '1.5rem' }}>
                            {cart.map((item) => (
                                <div key={item.productId} style={{
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'center',
                                    padding: '1rem',
                                    background: 'var(--gray-50)',
                                    borderRadius: 'var(--radius-lg)'
                                }}>
                                    <div>
                                        <strong>{item.productName}</strong>
                                        <div style={{ fontSize: '0.875rem', color: 'var(--gray-600)' }}>
                                            {item.price?.toFixed(2)} € × {item.quantity} = {(item.price * item.quantity).toFixed(2)} €
                                        </div>
                                    </div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        <button
                                            className="btn btn-secondary btn-sm"
                                            onClick={() => updateQuantity(item.productId, item.quantity - 1)}
                                        >
                                            -
                                        </button>
                                        <span style={{ minWidth: '2rem', textAlign: 'center' }}>{item.quantity}</span>
                                        <button
                                            className="btn btn-secondary btn-sm"
                                            onClick={() => updateQuantity(item.productId, item.quantity + 1)}
                                        >
                                            +
                                        </button>
                                        <button
                                            className="btn btn-danger btn-sm"
                                            onClick={() => removeFromCart(item.productId)}
                                        >
                                            🗑️
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>

                        <div style={{
                            borderTop: '2px solid var(--gray-200)',
                            paddingTop: '1rem',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center'
                        }}>
                            <div>
                                <div style={{ fontSize: '0.875rem', color: 'var(--gray-600)' }}>Total</div>
                                <div style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--primary-600)' }}>
                                    {getTotalPrice().toFixed(2)} €
                                </div>
                            </div>
                            <button
                                className="btn btn-success"
                                onClick={handleSubmit}
                                disabled={createOrderMutation.isPending}
                            >
                                {createOrderMutation.isPending ? 'Création...' : '✅ Commander'}
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}

export default CreateOrder;
