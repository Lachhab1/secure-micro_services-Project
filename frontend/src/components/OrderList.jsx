import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { orderApi } from '../services/api';
import { useAuth } from '../context/AuthContext';

/**
 * Liste des commandes (propres commandes pour CLIENT, toutes pour ADMIN).
 */
function OrderList() {
    const { isAdmin } = useAuth();
    const queryClient = useQueryClient();

    const { data: orders, isLoading, error } = useQuery({
        queryKey: ['orders', isAdmin() ? 'all' : 'my'],
        queryFn: async () => {
            const response = isAdmin() ? await orderApi.getAll() : await orderApi.getMyOrders();
            return response.data;
        }
    });

    const updateStatusMutation = useMutation({
        mutationFn: ({ id, status }) => orderApi.updateStatus(id, status),
        onSuccess: () => {
            queryClient.invalidateQueries(['orders']);
        }
    });

    const cancelMutation = useMutation({
        mutationFn: orderApi.cancel,
        onSuccess: () => {
            queryClient.invalidateQueries(['orders']);
        }
    });

    const getStatusClass = (status) => {
        const statusMap = {
            PENDING: 'pending',
            CONFIRMED: 'confirmed',
            PROCESSING: 'confirmed',
            SHIPPED: 'confirmed',
            DELIVERED: 'delivered',
            CANCELLED: 'cancelled'
        };
        return statusMap[status] || 'pending';
    };

    const getStatusLabel = (status) => {
        const labels = {
            PENDING: 'En attente',
            CONFIRMED: 'Confirmée',
            PROCESSING: 'En cours',
            SHIPPED: 'Expédiée',
            DELIVERED: 'Livrée',
            CANCELLED: 'Annulée'
        };
        return labels[status] || status;
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleDateString('fr-FR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    if (isLoading) {
        return <div className="loading"><div className="spinner"></div></div>;
    }

    if (error) {
        return (
            <div className="alert alert-error">
                <strong>Erreur:</strong> {error.response?.data?.message || 'Impossible de charger les commandes'}
            </div>
        );
    }

    return (
        <div>
            <div className="card" style={{ marginBottom: '1.5rem' }}>
                <div className="card-header">
                    <h2 className="card-title">📋 {isAdmin() ? 'Toutes les Commandes' : 'Mes Commandes'}</h2>
                    <span style={{ color: 'var(--gray-600)' }}>{orders?.length || 0} commande(s)</span>
                </div>
            </div>

            {orders?.length === 0 ? (
                <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
                    <p style={{ color: 'var(--gray-500)' }}>Aucune commande trouvée</p>
                </div>
            ) : (
                <div className="card">
                    <div className="table-container">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    {isAdmin() && <th>Client</th>}
                                    <th>Date</th>
                                    <th>Articles</th>
                                    <th>Total</th>
                                    <th>Statut</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {orders?.map((order) => (
                                    <tr key={order.id}>
                                        <td><strong>#{order.id}</strong></td>
                                        {isAdmin() && <td>{order.username}</td>}
                                        <td>{formatDate(order.orderDate)}</td>
                                        <td>
                                            {order.items?.length || 0} article(s)
                                            <ul style={{ fontSize: '0.75rem', color: 'var(--gray-500)', margin: '0.25rem 0 0 1rem', padding: 0 }}>
                                                {order.items?.slice(0, 2).map((item, idx) => (
                                                    <li key={idx}>{item.productName} x{item.quantity}</li>
                                                ))}
                                                {order.items?.length > 2 && <li>...</li>}
                                            </ul>
                                        </td>
                                        <td><strong>{order.totalAmount?.toFixed(2)} €</strong></td>
                                        <td>
                                            <span className={`order-status ${getStatusClass(order.status)}`}>
                                                {getStatusLabel(order.status)}
                                            </span>
                                        </td>
                                        <td>
                                            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                                                {isAdmin() && order.status !== 'CANCELLED' && order.status !== 'DELIVERED' && (
                                                    <select
                                                        className="form-input"
                                                        style={{ padding: '0.25rem', fontSize: '0.75rem', width: 'auto' }}
                                                        value={order.status}
                                                        onChange={(e) => updateStatusMutation.mutate({ id: order.id, status: e.target.value })}
                                                        disabled={updateStatusMutation.isPending}
                                                    >
                                                        <option value="PENDING">En attente</option>
                                                        <option value="CONFIRMED">Confirmée</option>
                                                        <option value="PROCESSING">En cours</option>
                                                        <option value="SHIPPED">Expédiée</option>
                                                        <option value="DELIVERED">Livrée</option>
                                                    </select>
                                                )}

                                                {order.status !== 'CANCELLED' && order.status !== 'DELIVERED' && (
                                                    <button
                                                        className="btn btn-danger btn-sm"
                                                        onClick={() => {
                                                            if (window.confirm('Êtes-vous sûr de vouloir annuler cette commande ?')) {
                                                                cancelMutation.mutate(order.id);
                                                            }
                                                        }}
                                                        disabled={cancelMutation.isPending}
                                                    >
                                                        Annuler
                                                    </button>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
}

export default OrderList;
