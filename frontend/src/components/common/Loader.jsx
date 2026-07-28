const Loader = ({ size = 'md', fullPage = false }) => {
  const ring = { sm: 'w-6 h-6 border-2', md: 'w-10 h-10 border-[3px]', lg: 'w-16 h-16 border-4' }

  const spinner = (
    <div className={`${ring[size] || ring.md} border-primary border-t-transparent rounded-full animate-spin`} role="status">
      <span className="sr-only">Loading…</span>
    </div>
  )

  if (fullPage) {
    return (
      <div className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-4"
        style={{ background: 'rgba(242,242,247,0.80)', backdropFilter: 'blur(16px)' }}>
        {spinner}
        <p className="text-secondary/60 font-medium text-sm animate-pulse">Loading…</p>
      </div>
    )
  }

  return <div className="flex justify-center items-center py-8">{spinner}</div>
}

export default Loader
