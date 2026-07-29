import { Link } from 'react-router-dom'

const NotFound = () => {
  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center py-12 text-center px-4">
      <div className="glass px-10 py-12 max-w-md w-full animate-scale-in">
        <p className="text-8xl font-black text-gradient mb-2">404</p>
        <h2 className="text-2xl font-black text-secondary mb-3">Page Not Found</h2>
        <p className="text-slate-500 mb-8">
          The page you&apos;re looking for doesn&apos;t exist or has been moved.
        </p>
        <Link to="/" className="btn-primary">
          Go Home
        </Link>
      </div>
    </div>
  )
}

export default NotFound
