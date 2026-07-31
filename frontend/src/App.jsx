import { BrowserRouter } from 'react-router-dom'
import AppRoutes from './routes/AppRoutes'
import { AuthProvider } from './context/AuthContext'
import { CartProvider } from './context/CartContext'
import { ToastProvider } from './context/ToastContext'
import { AddressProvider } from './context/AddressContext'
import ToastContainer from './components/common/Toast'

function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <AddressProvider>
          <ToastProvider>
            <BrowserRouter>
              <AppRoutes />
              <ToastContainer />
            </BrowserRouter>
          </ToastProvider>
        </AddressProvider>
      </CartProvider>
    </AuthProvider>
  )
}

export default App
