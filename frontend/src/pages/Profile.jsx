import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

const Profile = () => {
  const { user, login } = useAuth()
  const { success } = useToast()

  const [isEditing, setIsEditing] = useState(false)
  const [name, setName] = useState(user?.name || '')
  const [email, setEmail] = useState(user?.email || '')
  const [phone, setPhone] = useState(user?.phone || '+91 9876543210')
  const [address, setAddress] = useState(user?.address || '123 Food Street, Bangalore')

  const handleSave = (e) => {
    e.preventDefault()
    const updatedUser = { ...user, name, email, phone, address }
    login(updatedUser, localStorage.getItem('token'))
    setIsEditing(false)
    success('Profile updated successfully!')
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-secondary">Your Profile</h1>
        <button
          onClick={() => setIsEditing(!isEditing)}
          className={`px-5 py-2 rounded-lg font-semibold transition-colors ${
            isEditing
              ? 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              : 'btn-primary'
          }`}
        >
          {isEditing ? 'Cancel' : 'Edit Profile'}
        </button>
      </div>

      <div className="bg-white rounded-xl p-8 card-shadow">
        <div className="flex items-center space-x-6 mb-8 pb-6 border-b border-gray-100">
          <div className="w-24 h-24 bg-gradient-to-br from-primary to-orange-400 rounded-full flex items-center justify-center shadow-md">
            <span className="text-white text-4xl font-bold">
              {user?.name?.charAt(0)?.toUpperCase() || user?.email?.charAt(0)?.toUpperCase() || 'U'}
            </span>
          </div>
          <div>
            <h2 className="text-2xl font-bold text-secondary">
              {user?.name || 'User'}
            </h2>
            <p className="text-gray-500">{user?.email}</p>
            <span className="inline-block bg-orange-100 text-orange-700 text-xs font-semibold px-2.5 py-0.5 rounded mt-2">
              {user?.role || 'CUSTOMER'}
            </span>
          </div>
        </div>

        <form onSubmit={handleSave} className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-gray-700 font-medium mb-2">Full Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={!isEditing}
              className={`w-full px-4 py-3 border border-gray-300 rounded-lg transition-colors ${
                isEditing ? 'bg-white focus:ring-2 focus:ring-primary/50' : 'bg-gray-50 text-gray-600'
              }`}
            />
          </div>

          <div>
            <label className="block text-gray-700 font-medium mb-2">Email Address</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={!isEditing}
              className={`w-full px-4 py-3 border border-gray-300 rounded-lg transition-colors ${
                isEditing ? 'bg-white focus:ring-2 focus:ring-primary/50' : 'bg-gray-50 text-gray-600'
              }`}
            />
          </div>

          <div>
            <label className="block text-gray-700 font-medium mb-2">Phone Number</label>
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              disabled={!isEditing}
              className={`w-full px-4 py-3 border border-gray-300 rounded-lg transition-colors ${
                isEditing ? 'bg-white focus:ring-2 focus:ring-primary/50' : 'bg-gray-50 text-gray-600'
              }`}
            />
          </div>

          <div>
            <label className="block text-gray-700 font-medium mb-2">Default Address</label>
            <input
              type="text"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              disabled={!isEditing}
              className={`w-full px-4 py-3 border border-gray-300 rounded-lg transition-colors ${
                isEditing ? 'bg-white focus:ring-2 focus:ring-primary/50' : 'bg-gray-50 text-gray-600'
              }`}
            />
          </div>

          {isEditing && (
            <div className="md:col-span-2 flex justify-end mt-4">
              <button type="submit" className="btn-primary px-8">
                Save Changes
              </button>
            </div>
          )}
        </form>
      </div>
    </div>
  )
}

export default Profile
