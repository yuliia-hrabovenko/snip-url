import { useState } from 'react'
import './App.css'

export default function App() {
  const [longUrl, setLongUrl] = useState('')
  const [expiresAt, setExpiresAt] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [copied, setCopied] = useState(false)

  async function handleShorten(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    setResult(null)

    try {
      const params = new URLSearchParams({ longUrl })
      if (expiresAt) {
        params.append('expiresAtValue', Math.floor(new Date(expiresAt).getTime() / 1000))
      }
      const res = await fetch(`/api/v1/shorten?${params}`, { method: 'POST' })
      if (!res.ok) throw new Error(`Server error: ${res.status}`)
      setResult(await res.json())
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const shortUrl = result
    ? `${window.location.origin}/api/v1/${result.shortCode}`
    : ''

  function handleCopy() {
    navigator.clipboard.writeText(shortUrl)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="app">
      <div className="bg-orbs">
        <div className="orb orb-1" />
        <div className="orb orb-2" />
      </div>

      <main className="container">
        <header className="header">
          <div className="logo">⚡</div>
          <h1>Snip<span>URL</span></h1>
          <p className="tagline">Shorten links. Share fast.</p>
        </header>

        <div className="card">
          <form onSubmit={handleShorten} className="form">
            <input
              type="url"
              className="url-input"
              placeholder="Paste your long URL here…"
              value={longUrl}
              onChange={e => setLongUrl(e.target.value)}
              required
            />
            <div className="form-row">
              <div className="expiry-group">
                <label htmlFor="expires">Expires at <span>(optional)</span></label>
                <input
                  id="expires"
                  type="datetime-local"
                  className="date-input"
                  value={expiresAt}
                  onChange={e => setExpiresAt(e.target.value)}
                />
              </div>
              <button type="submit" disabled={loading} className="btn-primary">
                {loading ? <span className="spinner" /> : 'Shorten →'}
              </button>
            </div>
          </form>

          {error && (
            <div className="alert alert-error">
              <span>⚠</span> {error}
            </div>
          )}

          {result && (
            <div className="result">
              <p className="result-label">Your short link is ready</p>
              <div className="result-box">
                <a
                  href={shortUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="short-url"
                >
                  {shortUrl}
                </a>
                <button
                  onClick={handleCopy}
                  className={`btn-copy ${copied ? 'copied' : ''}`}
                >
                  {copied ? '✓ Copied' : 'Copy'}
                </button>
              </div>
              {result.expiresAt && (
                <div className="result-meta">
                  <span className="meta-expiry">
                    Expires {new Date(result.expiresAt).toLocaleDateString()}
                  </span>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="features">
          {[
            { icon: '🔗', label: 'Instant shortening' },
            { icon: '📊', label: 'Click tracking' },
            { icon: '⏱', label: 'Custom expiry' },
            { icon: '⚡', label: 'Kafka-powered' },
          ].map(f => (
            <div key={f.label} className="feature">
              <span>{f.icon}</span>
              <p>{f.label}</p>
            </div>
          ))}
        </div>
      </main>
    </div>
  )
}