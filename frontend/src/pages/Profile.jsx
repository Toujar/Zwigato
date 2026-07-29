import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

const ROLE_LABEL = {
  CUSTOMER:           { label: 'Customer',   class: 'badge-blue'   },
  ADMIN:              { label: 'Admin',       class: 'badge-orange' },
  RESTAURANT_OWNER:   { label: 'Owner',       class: 'badge-green'  },
  DELIVERY_AGENT:     { label: 'Agent',       class: 'badge-gray'   },
}

const Profile = () => {
  const { user, login } = useAuth()
  const { success }     = useToast()

  const [isEditing, setIsEditing] = useState(false)
  const [name, setName]           = useState(user?.name || '')
  const [email, setEmail]         = useState(user?.email || '')
  const [phone, setPhone]         = useState(user?.phone || '')
  const [address, setAddress]     = useState(user?.address || '')

  const handleSave = (e) => {
    e.preventDefault()
    const updatedUser = { ...user, name, email, phone, address }
    login(updatedUser, localStorage.getItem('token'))
    setIsEditing(false)
    success('Profile updated successfully!')
  }

  const roleInfo = ROLE_LABEL[user?.role] || ROLE_LABEL.CUSTOMER
  const initial  = user?.name?.charAt(0)?.toUpperCase() || user?.email?.charAt(0)?.toUpperCase() || 'U'

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Page header */}
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-black text-secondary">Your Profile</h1>
          <p className="text-slate-500 text-sm mt-1">Manage your account details</p>
        </div>
        <button
          onClick={() => setIsEditing(!isEditing)}
          className={isEditing ? 'btn-glass' : 'btn-primary'}
        >
          {isEditing ? 'Cancel' : 'Edit Profile'}
        </button>
      </div>

      {/* Profile card */}
      <div className="glass p-8">
        {/* Avatar row */}
        <div className="flex items-center gap-5 mb-8 pb-6"
          style={{ borderBottom: '1px solid rgba(186,230,253,0.45)' }}>
          <div className="w-20 h-20 rounded-2xl flex items-center justify-center shadow-glass shrink-0"
            style={{ background: 'linear-gradient(135deg,#0EA5E9 0%,#38BDF8 100%)' }}>
            <span className="text-white text-3xl font-black">{initial}</span>
          </div>
          <div>
            <h2 className="text-xl font-black text-secondary">{user?.name || 'User'}</h2>
            <p className="text-slate-500 text-sm">{user?.email}</p>
            <span className={`badge ${roleInfo.class} mt-2`}>{roleInfo.label}</span>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSave} className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {[
            { label: 'Full Name',       value: name,    setter: setName,    type: 'text',  auto: 'name' },
            { label: 'Email Address',   value: email,   setter: setEmail,   type: 'email', auto: 'email' },
            { label: 'Phone Number',    value: phone,   setter: setPhone,   type: 'tel',   auto: 'tel' },
            { label: 'Default Address', value: address, setter: setAddress, type: 'text',  auto: 'street-address' },
          ].map(({ label, value, setter, type, auto }) => (
            <div key={label}>
              <label className="block text-slate-700 font-semibold mb-1.5 text-sm">{label}</label>
              <input
                type={type} value={value} autoComplete={auto}
                onChange={(e) => setter(e.target.value)}
                disabled={!isEditing}
                className={isEditing ? 'input-glass' : 'input-glass opacity-60 cursor-not-allowed'}
              />
            </div>
          ))}

          {isEditing && (
            <div className="md:col-span-2 flex justify-end mt-2">
              <button type="submit" className="btn-primary px-10">
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
