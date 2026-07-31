import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import AddressManager from '../components/address/AddressManager'

const ROLE_LABEL = {
  CUSTOMER:         { label: 'Customer', class: 'badge-blue'   },
  ADMIN:            { label: 'Admin',    class: 'badge-orange' },
  RESTAURANT_OWNER: { label: 'Owner',    class: 'badge-green'  },
  DELIVERY_AGENT:   { label: 'Agent',    class: 'badge-gray'   },
}

const TABS = ['Profile', 'Addresses']

const Profile = () => {
  const { user, login } = useAuth()
  const { success }     = useToast()

  const [activeTab, setActiveTab] = useState('Profile')
  const [isEditing, setIsEditing] = useState(false)
  const [name, setName]           = useState(user?.name || '')
  const [email, setEmail]         = useState(user?.email || '')
  const [phone, setPhone]         = useState(user?.phone || '')

  const handleSave = (e) => {
    e.preventDefault()
    const updatedUser = { ...user, name, email, phone }
    login(updatedUser, localStorage.getItem('token'))
    setIsEditing(false)
    success('Profile updated!')
  }

  const roleInfo = ROLE_LABEL[user?.role] || ROLE_LABEL.CUSTOMER
  const initial  = user?.name?.charAt(0)?.toUpperCase() || user?.email?.charAt(0)?.toUpperCase() || 'U'

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-black text-secondary">Account</h1>
        <p className="text-slate-500 text-sm mt-1">Manage your profile and delivery addresses</p>
      </div>

      {/* Avatar card */}
      <div className="glass p-6 mb-6 flex items-center gap-5">
        <div className="w-16 h-16 rounded-2xl flex items-center justify-center shadow-glass shrink-0"
          style={{ background: 'linear-gradient(135deg,#0EA5E9 0%,#38BDF8 100%)' }}>
          <span className="text-white text-2xl font-black">{initial}</span>
        </div>
        <div className="flex-1 min-w-0">
          <h2 className="text-lg font-black text-secondary truncate">{user?.name || 'User'}</h2>
          <p className="text-slate-500 text-sm">{user?.email}</p>
          <span className={`badge ${roleInfo.class} mt-1`}>{roleInfo.label}</span>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-6 glass-subtle rounded-2xl p-1.5 w-fit">
        {TABS.map(tab => (
          <button key={tab} onClick={() => setActiveTab(tab)}
            className={`px-5 py-2 rounded-xl text-sm font-semibold transition-all ${
              activeTab === tab
                ? 'bg-white shadow-sm text-secondary'
                : 'text-slate-500 hover:text-secondary'
            }`}>
            {tab === 'Profile' ? '👤 Profile' : '📍 Addresses'}
          </button>
        ))}
      </div>

      {/* ── Profile Tab ── */}
      {activeTab === 'Profile' && (
        <div className="glass p-6">
          <div className="flex justify-between items-center mb-6">
            <h3 className="font-bold text-secondary">Personal Details</h3>
            <button onClick={() => setIsEditing(!isEditing)}
              className={isEditing ? 'btn-glass text-sm px-4 py-2' : 'btn-primary text-sm px-4 py-2'}>
              {isEditing ? 'Cancel' : 'Edit Profile'}
            </button>
          </div>

          <form onSubmit={handleSave} className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {[
              { label: 'Full Name',     value: name,  setter: setName,  type: 'text',  auto: 'name' },
              { label: 'Email Address', value: email, setter: setEmail, type: 'email', auto: 'email' },
              { label: 'Phone Number',  value: phone, setter: setPhone, type: 'tel',   auto: 'tel' },
            ].map(({ label, value, setter, type, auto }) => (
              <div key={label}>
                <label className="block text-slate-700 font-semibold mb-1.5 text-sm">{label}</label>
                <input type={type} value={value} autoComplete={auto}
                  onChange={e => setter(e.target.value)}
                  disabled={!isEditing}
                  className={`input-glass ${!isEditing ? 'opacity-60 cursor-not-allowed' : ''}`}
                />
              </div>
            ))}

            {isEditing && (
              <div className="md:col-span-2 flex justify-end mt-2">
                <button type="submit" className="btn-primary px-10">Save Changes</button>
              </div>
            )}
          </form>
        </div>
      )}

      {/* ── Addresses Tab ── */}
      {activeTab === 'Addresses' && (
        <div className="glass p-6">
          <AddressManager />
        </div>
      )}
    </div>
  )
}

export default Profile
