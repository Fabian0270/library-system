// auth.js - Autentiseringshjälpfunktioner

// Kontrollera om användaren är inloggad
async function checkAuth() {
    try {
        const response = await fetch('/api/auth/check', {
            credentials: 'include'
        });

        if (!response.ok) {
            return null;
        }

        const data = await response.json();

        if (data.authenticated) {
            // Hämta mer användarinfo om det behövs
            const userInfo = JSON.parse(sessionStorage.getItem('user') || '{}');
            return {
                ...data,
                ...userInfo
            };
        }

        return null;
    } catch (error) {
        console.error('Auth check failed:', error);
        return null;
    }
}

// Omdirigera till login om inte inloggad
async function requireAuth() {
    const user = await checkAuth();

    if (!user) {
        window.location.href = '/login.html?error=true';
        return null;
    }

    return user;
}

// Kontrollera om användaren har en specifik roll
function hasRole(user, role) {
    return user && user.roles && user.roles.includes(role);
}

// Logga ut
async function logout() {
    try {
        const response = await fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'include'
        });

        if (response.ok) {
            sessionStorage.clear();
            window.location.href = '/login.html?logout=true';
        } else {
            console.error('Logout failed');
        }
    } catch (error) {
        console.error('Logout error:', error);
    }
}

// API-anrop med autentisering
async function authenticatedFetch(url, options = {}) {
    const defaultOptions = {
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            ...options.headers
        }
    };

    const response = await fetch(url, { ...defaultOptions, ...options });

    // Om 401, omdirigera till login
    if (response.status === 401) {
        window.location.href = '/login.html?error=true';
        return null;
    }

    // Om 403, visa åtkomst nekad
    if (response.status === 403) {
        alert('Du har inte behörighet att utföra denna åtgärd');
        return null;
    }

    return response;
}

// Hämta CSRF-token om det behövs
async function getCsrfToken() {
    try {
        const response = await fetch('/api/auth/csrf', {
            credentials: 'include'
        });

        if (response.ok) {
            const data = await response.json();
            return data.token;
        }
    } catch (error) {
        console.error('Failed to get CSRF token:', error);
    }
    return null;
}

// Formatera datum
function formatDate(dateString) {
    if (!dateString) return 'Aldrig';

    const date = new Date(dateString);
    return date.toLocaleDateString('sv-SE', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Visa/dölj element baserat på roller
function updateUIBasedOnRoles(user) {
    // Visa/dölj USER-endast element
    const userElements = document.querySelectorAll('.user-only');
    userElements.forEach(el => {
        el.style.display = hasRole(user, 'USER') || hasRole(user, 'ADMIN') ? 'block' : 'none';
    });

    // Visa/dölj ADMIN-endast element
    const adminElements = document.querySelectorAll('.admin-only');
    adminElements.forEach(el => {
        el.style.display = hasRole(user, 'ADMIN') ? 'block' : 'none';
    });
}

// Visa notifikation
function showNotification(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
    alertDiv.style.zIndex = '9999';
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    document.body.appendChild(alertDiv);

    // Ta bort efter 5 sekunder
    setTimeout(() => {
        alertDiv.remove();
    }, 5000);
}

// Validera e-postadress
function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

// Validera lösenord enligt policy
function isValidPassword(password) {
    // Minst 8 tecken, innehåller både bokstäver och siffror
    return /^(?=.*[a-zA-Z])(?=.*\d).{8,}$/.test(password);
}