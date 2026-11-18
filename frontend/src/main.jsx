import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import axios from 'axios';
import { AlertCircle, Brain, CheckCircle2, Clock, Search, Sparkles, Target, XCircle } from 'lucide-react';
import './styles.css';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
});

const emptyForm = {
  resumeText: '',
  jobDescription: '',
};

function App() {
  const [form, setForm] = useState(emptyForm);
  const [currentAnalysis, setCurrentAnalysis] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [historyFailed, setHistoryFailed] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadHistory();
  }, []);

  const canAnalyze = useMemo(
    () => form.resumeText.trim().length > 0 && form.jobDescription.trim().length > 0 && !loading,
    [form, loading],
  );

  async function loadHistory() {
    try {
      setHistoryLoading(true);
      const { data } = await api.get('/analyses');
      setHistory(data);
      setHistoryFailed(false);
    } catch {
      setHistoryFailed(true);
      setError('Could not load previous analyses. Make sure the backend is running.');
    } finally {
      setHistoryLoading(false);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      const { data } = await api.post('/analyses', form);
      setCurrentAnalysis(data);
      setForm(emptyForm);
      await loadHistory();
    } catch (err) {
      setError(err.response?.data?.message || 'Could not analyze the resume. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  async function openAnalysis(id) {
    setError('');
    try {
      const { data } = await api.get(`/analyses/${id}`);
      setCurrentAnalysis(data);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch {
      setError('Could not open that analysis.');
    }
  }

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">AI Resume Matcher</p>
          <h1>CareerFit AI</h1>
          <p>Understand how well your resume matches a job.</p>
        </div>
        <div className="header-mark" aria-hidden="true">
          <Brain size={34} />
        </div>
      </header>

      {error && (
        <div className="alert" role="alert">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      <section className="workspace-grid">
        <form className="input-panel" onSubmit={handleSubmit}>
          <div className="section-title">
            <Sparkles size={20} />
            <h2>Analyze a Resume</h2>
          </div>

          <label>
            <span>Your Resume</span>
            <textarea
              value={form.resumeText}
              onChange={(event) => setForm({ ...form, resumeText: event.target.value })}
              placeholder="Paste your resume here"
              rows={12}
            />
          </label>

          <label>
            <span>Job Description</span>
            <textarea
              value={form.jobDescription}
              onChange={(event) => setForm({ ...form, jobDescription: event.target.value })}
              placeholder="Paste the job description here"
              rows={12}
            />
          </label>

          <button type="submit" disabled={!canAnalyze}>
            {loading ? <Clock size={18} className="spin" /> : <Search size={18} />}
            {loading ? 'Analyzing...' : 'Analyze Resume'}
          </button>
        </form>

        <section className="results-panel">
          {currentAnalysis ? <Results analysis={currentAnalysis} /> : <EmptyResults />}
        </section>
      </section>

      <section className="history-panel">
        <div className="section-title">
          <Target size={20} />
          <h2>Analysis History</h2>
        </div>

        {historyLoading ? (
          <p className="empty-text">Loading previous analyses...</p>
        ) : historyFailed ? (
          <p className="empty-text">History is unavailable while the backend is offline.</p>
        ) : history.length === 0 ? (
          <p className="empty-text">No analyses yet. Run your first comparison to build a history.</p>
        ) : (
          <div className="history-list">
            {history.map((item) => (
              <button className="history-item" key={item.id} onClick={() => openAnalysis(item.id)}>
                <span className="history-id">#{item.id}</span>
                <strong>{item.matchScore}% match</strong>
                <span>{formatDate(item.createdAt)}</span>
                <p>{item.jobDescriptionPreview}</p>
              </button>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

function Results({ analysis }) {
  return (
    <div className="results-stack">
      <div className="score-card">
        <div className="score-ring">
          <span>{analysis.matchScore}%</span>
        </div>
        <div>
          <p className="eyebrow">Match Score</p>
          <h2>{scoreLabel(analysis.matchScore)}</h2>
          <p className="muted">Saved {formatDate(analysis.createdAt)}</p>
        </div>
      </div>

      <ResultCard icon={<CheckCircle2 size={20} />} title="Matching Skills">
        <BadgeList items={analysis.matchingSkills} tone="good" empty="No overlapping skills were identified." />
      </ResultCard>

      <ResultCard icon={<XCircle size={20} />} title="Missing Skills">
        <BadgeList items={analysis.missingSkills} tone="warn" empty="No major missing skills were identified." />
      </ResultCard>

      <ResultCard icon={<Sparkles size={20} />} title="AI Suggestions">
        {analysis.suggestions?.length ? (
          <ol className="suggestions">
            {analysis.suggestions.map((suggestion, index) => (
              <li key={`${suggestion}-${index}`}>{suggestion}</li>
            ))}
          </ol>
        ) : (
          <p className="empty-text">No suggestions returned.</p>
        )}
      </ResultCard>

      <ResultCard icon={<Target size={20} />} title="Recommended Keywords">
        <BadgeList items={analysis.recommendedKeywords} tone="neutral" empty="No keywords returned." />
      </ResultCard>

      <ResultCard icon={<Search size={20} />} title="Skill Gap Roadmap">
        {analysis.skillGapRoadmap?.length ? (
          <div className="roadmap">
            <div className="roadmap-head">
              <span>Skill</span>
              <span>Priority</span>
              <span>Reason</span>
            </div>
            {analysis.skillGapRoadmap.map((item, index) => (
              <div className="roadmap-row" key={`${item.skill}-${index}`}>
                <strong>{item.skill}</strong>
                <span className={`priority ${item.priority?.toLowerCase()}`}>{item.priority}</span>
                <p>{item.reason}</p>
              </div>
            ))}
          </div>
        ) : (
          <p className="empty-text">No roadmap items returned.</p>
        )}
      </ResultCard>
    </div>
  );
}

function EmptyResults() {
  return (
    <div className="empty-results">
      <div className="header-mark">
        <Sparkles size={32} />
      </div>
      <h2>Your analysis will appear here</h2>
      <p>Paste a resume and job description to see matching skills, gaps, keywords, and next steps.</p>
    </div>
  );
}

function ResultCard({ icon, title, children }) {
  return (
    <article className="result-card">
      <div className="section-title compact">
        {icon}
        <h3>{title}</h3>
      </div>
      {children}
    </article>
  );
}

function BadgeList({ items = [], tone, empty }) {
  if (!items.length) {
    return <p className="empty-text">{empty}</p>;
  }

  return (
    <div className="badges">
      {items.map((item) => (
        <span className={`badge ${tone}`} key={item}>
          {item}
        </span>
      ))}
    </div>
  );
}

function formatDate(value) {
  if (!value) {
    return 'Unknown date';
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function scoreLabel(score) {
  if (score >= 80) return 'Strong alignment';
  if (score >= 60) return 'Good foundation';
  if (score >= 40) return 'Some alignment';
  return 'Needs tailoring';
}

createRoot(document.getElementById('root')).render(<App />);
