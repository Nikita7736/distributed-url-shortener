async function createShortUrl(event) {
    event.preventDefault();

    const urlInput = document.getElementById('url-input');
    const ttlInput = document.getElementById('ttl-input');
    const resultDiv = document.getElementById('create-result');

    const payload = { url: urlInput.value.trim() };
    const ttl = ttlInput.value.trim();
    if (ttl) {
        payload.ttlSeconds = parseInt(ttl, 10);
    }

    resultDiv.classList.remove('result-error');
    resultDiv.classList.remove('hidden');
    resultDiv.textContent = 'Creating short URL...';

    try {
        const response = await fetch('/api/urls', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorBody = await response.json().catch(() => ({}));
            const message = errorBody.error || `Request failed with status ${response.status}`;
            throw new Error(message);
        }

        const data = await response.json();
        const ttlSeconds = data.ttlSeconds != null ? data.ttlSeconds : '∞';

        resultDiv.innerHTML = `
            <div class="result-row">
              <div class="result-label">Short URL</div>
              <div class="result-value"><a href="${data.shortUrl}" target="_blank" rel="noopener noreferrer">${data.shortUrl}</a></div>
            </div>
            <div class="result-row">
              <div class="result-label">Code</div>
              <div class="result-value">${data.code}</div>
            </div>
            <div class="result-row">
              <div class="result-label">Original URL</div>
              <div class="result-value">${data.originalUrl}</div>
            </div>
            <div class="result-row">
              <div class="result-label">TTL (seconds)</div>
              <div class="result-value">${ttlSeconds}</div>
            </div>
        `;
    } catch (err) {
        resultDiv.classList.add('result-error');
        resultDiv.textContent = err.message || 'Failed to create short URL.';
    }
}

async function getAnalytics(event) {
    event.preventDefault();

    const codeInput = document.getElementById('code-input');
    const resultDiv = document.getElementById('analytics-result');

    const code = codeInput.value.trim();
    if (!code) {
        return;
    }

    resultDiv.classList.remove('result-error');
    resultDiv.classList.remove('hidden');
    resultDiv.textContent = 'Loading analytics...';

    try {
        const response = await fetch(`/api/urls/${encodeURIComponent(code)}`);

        if (response.status === 404) {
            throw new Error('Short URL not found or expired.');
        }
        if (!response.ok) {
            throw new Error(`Request failed with status ${response.status}`);
        }

        const data = await response.json();
        resultDiv.innerHTML = `
            <div class="result-row">
              <div class="result-label">Code</div>
              <div class="result-value">${data.code}</div>
            </div>
            <div class="result-row">
              <div class="result-label">Original URL</div>
              <div class="result-value">${data.originalUrl}</div>
            </div>
            <div class="result-row">
              <div class="result-label">Clicks</div>
              <div class="result-value">${data.clickCount}</div>
            </div>
            <div class="result-row">
              <div class="result-label">Created at</div>
              <div class="result-value">${data.createdAt}</div>
            </div>
            <div class="result-row">
              <div class="result-label">Expires at</div>
              <div class="result-value">${data.expiresAt ?? 'No expiry'}</div>
            </div>
        `;
    } catch (err) {
        resultDiv.classList.add('result-error');
        resultDiv.textContent = err.message || 'Failed to load analytics.';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const createForm = document.getElementById('create-form');
    const analyticsForm = document.getElementById('analytics-form');

    createForm.addEventListener('submit', createShortUrl);
    analyticsForm.addEventListener('submit', getAnalytics);
});

