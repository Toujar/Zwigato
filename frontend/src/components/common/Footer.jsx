const Footer = () => (
  <footer className="mt-20 glass-dark">
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
        <div className="col-span-2 md:col-span-1">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-9 h-9 rounded-2xl flex items-center justify-center"
              style={{ background: 'linear-gradient(135deg,#0EA5E9,#38BDF8)' }}>
              <span className="text-white font-black text-base">Z</span>
            </div>
            <span className="text-xl font-black text-white">Zwigato</span>
          </div>
          <p className="text-sky-200/55 text-sm leading-relaxed">
            Delicious food delivered fast. Order from the best restaurants near you.
          </p>
        </div>
        {[
          { title: 'Company', links: ['About Us','Careers','Blog','Press'] },
          { title: 'Support', links: ['Help Center','Contact Us','Safety','FAQ'] },
          { title: 'Legal',   links: ['Privacy Policy','Terms of Service','Cookie Policy','Refunds'] },
        ].map(({ title, links }) => (
          <div key={title}>
            <h4 className="text-white font-semibold mb-4 text-xs uppercase tracking-widest opacity-60">{title}</h4>
            <ul className="space-y-2">
              {links.map(l => (
                <li key={l}>
                  <a href="#" className="text-sky-200/45 text-sm hover:text-sky-300 transition-colors">{l}</a>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <div className="mt-10 pt-6" style={{ borderTop: '1px solid rgba(186,230,253,0.12)' }}>
        <div className="flex flex-col sm:flex-row justify-between items-center gap-3">
          <p className="text-sky-200/30 text-sm">© 2025 Zwigato. All rights reserved.</p>
          <div className="flex gap-4 text-xl">
            {['🍕','🍔','🍣','🌮','🍜'].map((e,i) => (
              <span key={i} className="hover:scale-125 transition-transform cursor-default select-none">{e}</span>
            ))}
          </div>
        </div>
      </div>
    </div>
  </footer>
)

export default Footer
