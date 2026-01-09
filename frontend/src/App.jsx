import { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Header from './components/Header';
import Navigation from './components/Navigation';
import ProductCatalog from './components/ProductCatalog';
import OrderList from './components/OrderList';
import CreateOrder from './components/CreateOrder';
import ProductForm from './components/ProductForm';

function App() {
    const { isAuthenticated, isAdmin } = useAuth();
    const [activeTab, setActiveTab] = useState('products');

    if (!isAuthenticated) {
        return <div className="loading"><div className="spinner"></div></div>;
    }

    return (
        <Router>
            <div className="app-container">
                <Header />
                <Navigation activeTab={activeTab} setActiveTab={setActiveTab} />

                <main>
                    <Routes>
                        <Route path="/" element={<Navigate to="/products" replace />} />
                        <Route path="/products" element={<ProductCatalog />} />
                        <Route path="/orders" element={<OrderList />} />
                        <Route path="/orders/new" element={<CreateOrder />} />
                        {isAdmin() && (
                            <>
                                <Route path="/products/new" element={<ProductForm />} />
                                <Route path="/products/:id/edit" element={<ProductForm />} />
                            </>
                        )}
                        <Route path="*" element={<Navigate to="/products" replace />} />
                    </Routes>
                </main>
            </div>
        </Router>
    );
}

export default App;
