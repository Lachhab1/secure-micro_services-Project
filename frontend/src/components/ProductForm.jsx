import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { productApi } from '../services/api';

/**
 * Formulaire de création/modification de produit (ADMIN).
 */
function ProductForm() {
    const { id } = useParams();
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const isEditing = Boolean(id);

    const [formData, setFormData] = useState({
        name: '',
        description: '',
        price: '',
        stockQuantity: ''
    });
    const [errors, setErrors] = useState({});

    // Charger le produit existant en mode édition
    const { data: existingProduct, isLoading: loadingProduct } = useQuery({
        queryKey: ['product', id],
        queryFn: async () => {
            const response = await productApi.getById(id);
            return response.data;
        },
        enabled: isEditing
    });

    useEffect(() => {
        if (existingProduct) {
            setFormData({
                name: existingProduct.name || '',
                description: existingProduct.description || '',
                price: existingProduct.price?.toString() || '',
                stockQuantity: existingProduct.stockQuantity?.toString() || ''
            });
        }
    }, [existingProduct]);

    const createMutation = useMutation({
        mutationFn: productApi.create,
        onSuccess: () => {
            queryClient.invalidateQueries(['products']);
            navigate('/products');
        }
    });

    const updateMutation = useMutation({
        mutationFn: ({ id, product }) => productApi.update(id, product),
        onSuccess: () => {
            queryClient.invalidateQueries(['products']);
            navigate('/products');
        }
    });

    const validate = () => {
        const newErrors = {};

        if (!formData.name.trim()) {
            newErrors.name = 'Le nom est obligatoire';
        } else if (formData.name.length < 2) {
            newErrors.name = 'Le nom doit contenir au moins 2 caractères';
        }

        if (!formData.price) {
            newErrors.price = 'Le prix est obligatoire';
        } else if (parseFloat(formData.price) <= 0) {
            newErrors.price = 'Le prix doit être supérieur à 0';
        }

        if (!formData.stockQuantity) {
            newErrors.stockQuantity = 'La quantité est obligatoire';
        } else if (parseInt(formData.stockQuantity) < 0) {
            newErrors.stockQuantity = 'La quantité ne peut pas être négative';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        if (!validate()) return;

        const productData = {
            name: formData.name.trim(),
            description: formData.description.trim(),
            price: parseFloat(formData.price),
            stockQuantity: parseInt(formData.stockQuantity)
        };

        if (isEditing) {
            updateMutation.mutate({ id, product: productData });
        } else {
            createMutation.mutate(productData);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        // Effacer l'erreur du champ modifié
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: null }));
        }
    };

    const isSubmitting = createMutation.isPending || updateMutation.isPending;
    const mutationError = createMutation.error || updateMutation.error;

    if (loadingProduct) {
        return <div className="loading"><div className="spinner"></div></div>;
    }

    return (
        <div className="card" style={{ maxWidth: '600px', margin: '0 auto' }}>
            <div className="card-header">
                <h2 className="card-title">
                    {isEditing ? '✏️ Modifier le produit' : '➕ Nouveau produit'}
                </h2>
            </div>

            {mutationError && (
                <div className="alert alert-error">
                    {mutationError.response?.data?.message || 'Une erreur est survenue'}
                </div>
            )}

            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label className="form-label" htmlFor="name">Nom du produit *</label>
                    <input
                        type="text"
                        id="name"
                        name="name"
                        className="form-input"
                        value={formData.name}
                        onChange={handleChange}
                        placeholder="Ex: Smartphone XYZ"
                    />
                    {errors.name && <span style={{ color: 'var(--error-500)', fontSize: '0.875rem' }}>{errors.name}</span>}
                </div>

                <div className="form-group">
                    <label className="form-label" htmlFor="description">Description</label>
                    <textarea
                        id="description"
                        name="description"
                        className="form-input"
                        rows="3"
                        value={formData.description}
                        onChange={handleChange}
                        placeholder="Description détaillée du produit..."
                    />
                </div>

                <div className="form-group">
                    <label className="form-label" htmlFor="price">Prix (€) *</label>
                    <input
                        type="number"
                        id="price"
                        name="price"
                        className="form-input"
                        step="0.01"
                        min="0.01"
                        value={formData.price}
                        onChange={handleChange}
                        placeholder="99.99"
                    />
                    {errors.price && <span style={{ color: 'var(--error-500)', fontSize: '0.875rem' }}>{errors.price}</span>}
                </div>

                <div className="form-group">
                    <label className="form-label" htmlFor="stockQuantity">Quantité en stock *</label>
                    <input
                        type="number"
                        id="stockQuantity"
                        name="stockQuantity"
                        className="form-input"
                        min="0"
                        value={formData.stockQuantity}
                        onChange={handleChange}
                        placeholder="100"
                    />
                    {errors.stockQuantity && <span style={{ color: 'var(--error-500)', fontSize: '0.875rem' }}>{errors.stockQuantity}</span>}
                </div>

                <div style={{ display: 'flex', gap: '1rem', marginTop: '1.5rem' }}>
                    <button
                        type="submit"
                        className="btn btn-primary"
                        disabled={isSubmitting}
                    >
                        {isSubmitting ? 'Enregistrement...' : (isEditing ? 'Mettre à jour' : 'Créer le produit')}
                    </button>
                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() => navigate('/products')}
                        disabled={isSubmitting}
                    >
                        Annuler
                    </button>
                </div>
            </form>
        </div>
    );
}

export default ProductForm;
