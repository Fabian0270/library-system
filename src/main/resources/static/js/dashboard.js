// dashboard.js - Dashboard funktionalitet

// Initiera dashboard när sidan laddas
document.addEventListener('DOMContentLoaded', async () => {
    const user = await requireAuth();

    if (user) {
        // Uppdatera användarinfo i UI
        updateUserInfo(user);

        // Uppdatera UI baserat på roller
        updateUIBasedOnRoles(user);

        // Ladda statistik för admins
        if (hasRole(user, 'ADMIN')) {
            loadAdminStatistics();
        }
    }
});

// Uppdatera användarinformation i UI
function updateUserInfo(user) {
    document.getElementById('userEmail').textContent = user.email || user.username;
    document.getElementById('userEmailInfo').textContent = user.email || user.username;
    document.getElementById('userName').textContent = user.firstName || 'Användare';

    // Visa roller
    const rolesDiv = document.getElementById('userRoles');
    if (rolesDiv && user.roles) {
        rolesDiv.innerHTML = user.roles.map(role =>
            `<span class="role-badge">${role}</span>`
        ).join('');
    }
}

// Ladda böcker
async function loadBooks() {
    const contentArea = document.getElementById('contentArea');
    contentArea.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"></div></div>';

    try {
        const response = await authenticatedFetch('/books');

        if (!response) return;

        const books = await response.json();

        let html = `
            <h3>Böcker i biblioteket</h3>
            <div class="mb-3">
                <input type="text" class="form-control" id="searchBooks" placeholder="Sök böcker..." onkeyup="filterBooks()">
            </div>
            <div class="table-responsive">
                <table class="table table-hover" id="booksTable">
                    <thead>
                        <tr>
                            <th>Titel</th>
                            <th>Publiceringsår</th>
                            <th>Tillgängliga</th>
                            <th>Totalt</th>
                            <th>Åtgärder</th>
                        </tr>
                    </thead>
                    <tbody>
        `;

        books.forEach(book => {
            const available = book.availableCopies > 0;
            html += `
                <tr>
                    <td>${book.title}</td>
                    <td>${book.publicationYear || '-'}</td>
                    <td>
                        <span class="badge ${available ? 'bg-success' : 'bg-danger'}">
                            ${book.availableCopies}
                        </span>
                    </td>
                    <td>${book.totalCopies}</td>
                    <td>
                        <button class="btn btn-sm btn-primary"
                                onclick="borrowBook(${book.bookId})"
                                ${!available ? 'disabled' : ''}>
                            ${available ? 'Låna' : 'Slut'}
                        </button>
                    </td>
                </tr>
            `;
        });

        html += `
                    </tbody>
                </table>
            </div>
        `;

        contentArea.innerHTML = html;

    } catch (error) {
        console.error('Error loading books:', error);
        contentArea.innerHTML = '<div class="alert alert-danger">Kunde inte ladda böcker</div>';
    }
}

// Filtrera böcker
function filterBooks() {
    const searchValue = document.getElementById('searchBooks').value.toLowerCase();
    const table = document.getElementById('booksTable');
    const rows = table.getElementsByTagName('tr');

    for (let i = 1; i < rows.length; i++) {
        const title = rows[i].getElementsByTagName('td')[0].textContent.toLowerCase();

        if (title.includes(searchValue)) {
            rows[i].style.display = '';
        } else {
            rows[i].style.display = 'none';
        }
    }
}

// Låna bok
async function borrowBook(bookId) {
    const user = await checkAuth();

    if (!user) {
        window.location.href = '/login.html';
        return;
    }

    if (confirm('Vill du låna denna bok?')) {
        try {
            const response = await authenticatedFetch('/loans', {
                method: 'POST',
                body: JSON.stringify({
                    userId: user.userId || 1, // Fallback om userId inte finns
                    bookId: bookId
                })
            });

            if (response && response.ok) {
                showNotification('Boken har lånats!', 'success');
                loadBooks(); // Ladda om böcker för att uppdatera tillgänglighet
            } else {
                const error = await response.json();
                showNotification(error.message || 'Kunde inte låna boken', 'danger');
            }
        } catch (error) {
            console.error('Error borrowing book:', error);
            showNotification('Ett fel uppstod', 'danger');
        }
    }
}

// Ladda lån
async function loadLoans() {
    const contentArea = document.getElementById('contentArea');
    contentArea.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"></div></div>';

    const user = await checkAuth();

    try {
        const response = await authenticatedFetch(`/users/${user.userId || 1}/loans`);

        if (!response) return;

        const loans = await response.json();

        let html = `
            <h3>Mina lån</h3>
            <div class="table-responsive">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>Lån ID</th>
                            <th>Bok ID</th>
                            <th>Lånad datum</th>
                            <th>Återlämnas senast</th>
                            <th>Status</th>
                            <th>Åtgärder</th>
                        </tr>
                    </thead>
                    <tbody>
        `;

        loans.forEach(loan => {
            const isOverdue = new Date(loan.dueDate) < new Date() && !loan.returnedDate;
            const isReturned = loan.returnedDate !== null;

            html += `
                <tr class="${isOverdue ? 'table-danger' : ''}">
                    <td>${loan.loanId}</td>
                    <td>${loan.bookId}</td>
                    <td>${formatDate(loan.borrowedDate)}</td>
                    <td>${formatDate(loan.dueDate)}</td>
                    <td>
                        ${isReturned
                            ? `<span class="badge bg-success">Återlämnad</span>`
                            : isOverdue
                                ? `<span class="badge bg-danger">Försenad</span>`
                                : `<span class="badge bg-warning">Aktiv</span>`
                        }
                    </td>
                    <td>
                        ${!isReturned ? `
                            <button class="btn btn-sm btn-success" onclick="returnBook(${loan.loanId})">
                                Återlämna
                            </button>
                            ${!isOverdue ? `
                                <button class="btn btn-sm btn-info" onclick="extendLoan(${loan.loanId})">
                                    Förläng
                                </button>
                            ` : ''}
                        ` : '-'}
                    </td>
                </tr>
            `;
        });

        html += `
                    </tbody>
                </table>
            </div>
        `;

        if (loans.length === 0) {
            html = '<div class="alert alert-info">Du har inga aktiva lån</div>';
        }

        contentArea.innerHTML = html;

    } catch (error) {
        console.error('Error loading loans:', error);
        contentArea.innerHTML = '<div class="alert alert-danger">Kunde inte ladda lån</div>';
    }
}

// Återlämna bok
async function returnBook(loanId) {
    if (confirm('Vill du återlämna denna bok?')) {
        try {
            const response = await authenticatedFetch(`/loans/${loanId}/return`, {
                method: 'PUT'
            });

            if (response && response.ok) {
                showNotification('Boken har återlämnats!', 'success');
                loadLoans();
            } else {
                showNotification('Kunde inte återlämna boken', 'danger');
            }
        } catch (error) {
            console.error('Error returning book:', error);
            showNotification('Ett fel uppstod', 'danger');
        }
    }
}

// Förläng lån
async function extendLoan(loanId) {
    if (confirm('Vill du förlänga lånet med 14 dagar?')) {
        try {
            const response = await authenticatedFetch(`/loans/${loanId}/extend`, {
                method: 'PUT'
            });

            if (response && response.ok) {
                showNotification('Lånet har förlängts!', 'success');
                loadLoans();
            } else {
                showNotification('Kunde inte förlänga lånet', 'danger');
            }
        } catch (error) {
            console.error('Error extending loan:', error);
            showNotification('Ett fel uppstod', 'danger');
        }
    }
}

// Visa profil
async function showProfile() {
    const contentArea = document.getElementById('contentArea');
    const user = await checkAuth();

    if (!user) return;

    contentArea.innerHTML = `
        <div class="card">
            <div class="card-header">
                <h3>Min Profil</h3>
            </div>
            <div class="card-body">
                <div class="row">
                    <div class="col-md-6">
                        <p><strong>Namn:</strong> ${user.firstName || '-'} ${user.lastName || '-'}</p>
                        <p><strong>E-post:</strong> ${user.email || user.username}</p>
                        <p><strong>Roller:</strong> ${user.roles ? user.roles.join(', ') : '-'}</p>
                    </div>
                    <div class="col-md-6">
                        <p><strong>Registrerad:</strong> ${formatDate(user.registrationDate)}</p>
                        <p><strong>Senaste inloggning:</strong> ${formatDate(user.lastLogin)}</p>
                    </div>
                </div>
                <hr>
                <h5>Säkerhetsinställningar</h5>
                <button class="btn btn-warning" onclick="showChangePassword()">Ändra lösenord</button>
            </div>
        </div>
    `;
}

// Visa formulär för lösenordsändring
function showChangePassword() {
    const contentArea = document.getElementById('contentArea');

    contentArea.innerHTML = `
        <div class="card">
            <div class="card-header">
                <h3>Ändra lösenord</h3>
            </div>
            <div class="card-body">
                <form id="changePasswordForm">
                    <div class="mb-3">
                        <label for="currentPassword" class="form-label">Nuvarande lösenord</label>
                        <input type="password" class="form-control" id="currentPassword" required>
                    </div>
                    <div class="mb-3">
                        <label for="newPassword" class="form-label">Nytt lösenord</label>
                        <input type="password" class="form-control" id="newPassword" required>
                        <div class="form-text">Minst 8 tecken, måste innehålla både bokstäver och siffror</div>
                    </div>
                    <div class="mb-3">
                        <label for="confirmNewPassword" class="form-label">Bekräfta nytt lösenord</label>
                        <input type="password" class="form-control" id="confirmNewPassword" required>
                    </div>
                    <button type="submit" class="btn btn-primary">Ändra lösenord</button>
                    <button type="button" class="btn btn-secondary" onclick="showProfile()">Avbryt</button>
                </form>
            </div>
        </div>
    `;

    document.getElementById('changePasswordForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        // Här skulle man implementera lösenordsändring
        showNotification('Lösenordsändring är inte implementerad än', 'info');
    });
}

// Ladda adminpanel
async function loadAdminPanel() {
    const contentArea = document.getElementById('contentArea');

    contentArea.innerHTML = `
        <h3>Administratörspanel</h3>
        <div class="row mb-4">
            <div class="col-md-3">
                <button class="btn btn-primary w-100" onclick="loadAllUsers()">
                    <i class="bi bi-people"></i> Hantera användare
                </button>
            </div>
            <div class="col-md-3">
                <button class="btn btn-info w-100" onclick="loadSecurityLogs()">
                    <i class="bi bi-shield-check"></i> Säkerhetsloggar
                </button>
            </div>
            <div class="col-md-3">
                <button class="btn btn-success w-100" onclick="loadAllLoans()">
                    <i class="bi bi-journal-text"></i> Alla lån
                </button>
            </div>
            <div class="col-md-3">
                <button class="btn btn-warning w-100" onclick="loadOverdueLoans()">
                    <i class="bi bi-exclamation-triangle"></i> Försenade lån
                </button>
            </div>
        </div>
        <div id="adminContent"></div>
    `;

    // Ladda statistik automatiskt
    loadAdminStatistics();
}

// Ladda alla användare (endast admin)
async function loadAllUsers() {
    const adminContent = document.getElementById('adminContent');
    adminContent.innerHTML = '<div class="spinner-border" role="status"></div>';

    try {
        const response = await authenticatedFetch('/users');

        if (!response) return;

        const users = await response.json();

        let html = `
            <h4>Registrerade användare</h4>
            <div class="table-responsive">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Namn</th>
                            <th>E-post</th>
                            <th>Registrerad</th>
                            <th>Roller</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
        `;

        users.forEach(user => {
            html += `
                <tr>
                    <td>${user.userId}</td>
                    <td>${user.firstName} ${user.lastName}</td>
                    <td>${user.email}</td>
                    <td>${formatDate(user.registrationDate)}</td>
                    <td>${user.roles ? user.roles.join(', ') : 'USER'}</td>
                    <td>
                        <span class="badge ${user.enabled ? 'bg-success' : 'bg-danger'}">
                            ${user.enabled ? 'Aktiv' : 'Inaktiv'}
                        </span>
                    </td>
                </tr>
            `;
        });

        html += `
                    </tbody>
                </table>
            </div>
        `;

        adminContent.innerHTML = html;

    } catch (error) {
        console.error('Error loading users:', error);
        adminContent.innerHTML = '<div class="alert alert-danger">Kunde inte ladda användare</div>';
    }
}

// Ladda säkerhetsloggar
async function loadSecurityLogs() {
    const adminContent = document.getElementById('adminContent');

    adminContent.innerHTML = `
        <h4>Säkerhetsloggar</h4>
        <div class="alert alert-info">
            Säkerhetsloggar visar inloggningsförsök, registreringar och andra säkerhetshändelser.
            <br>För fullständiga loggar, se filen: <code>logs/library-system.log</code>
        </div>
        <p>Denna funktion kräver ytterligare implementation av en endpoint för att hämta säkerhetsloggar.</p>
    `;
}

// Ladda alla lån
async function loadAllLoans() {
    const adminContent = document.getElementById('adminContent');
    adminContent.innerHTML = '<div class="spinner-border" role="status"></div>';

    try {
        const response = await authenticatedFetch('/loans');

        if (!response) return;

        const loans = await response.json();

        let html = `
            <h4>Alla lån i systemet</h4>
            <div class="table-responsive">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>Lån ID</th>
                            <th>Användare ID</th>
                            <th>Bok ID</th>
                            <th>Lånad</th>
                            <th>Återlämnas</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
        `;

        loans.forEach(loan => {
            const isOverdue = new Date(loan.dueDate) < new Date() && !loan.returnedDate;
            const isReturned = loan.returnedDate !== null;

            html += `
                <tr class="${isOverdue ? 'table-danger' : ''}">
                    <td>${loan.loanId}</td>
                    <td>${loan.userId}</td>
                    <td>${loan.bookId}</td>
                    <td>${formatDate(loan.borrowedDate)}</td>
                    <td>${formatDate(loan.dueDate)}</td>
                    <td>
                        ${isReturned
                            ? `<span class="badge bg-success">Återlämnad</span>`
                            : isOverdue
                                ? `<span class="badge bg-danger">Försenad</span>`
                                : `<span class="badge bg-primary">Aktiv</span>`
                        }
                    </td>
                </tr>
            `;
        });

        html += `
                    </tbody>
                </table>
            </div>
        `;

        adminContent.innerHTML = html;

    } catch (error) {
        console.error('Error loading loans:', error);
        adminContent.innerHTML = '<div class="alert alert-danger">Kunde inte ladda lån</div>';
    }
}

// Ladda försenade lån
async function loadOverdueLoans() {
    const adminContent = document.getElementById('adminContent');
    adminContent.innerHTML = '<div class="spinner-border" role="status"></div>';

    try {
        const response = await authenticatedFetch('/loans/overdue');

        if (!response) return;

        const loans = await response.json();

        if (loans.length === 0) {
            adminContent.innerHTML = '<div class="alert alert-success">Inga försenade lån!</div>';
            return;
        }

        let html = `
            <h4>Försenade lån</h4>
            <div class="alert alert-warning">
                Det finns ${loans.length} försenade lån som behöver åtgärdas.
            </div>
            <div class="table-responsive">
                <table class="table table-hover table-danger">
                    <thead>
                        <tr>
                            <th>Lån ID</th>
                            <th>Användare ID</th>
                            <th>Bok ID</th>
                            <th>Skulle återlämnas</th>
                            <th>Dagar försenat</th>
                        </tr>
                    </thead>
                    <tbody>
        `;

        loans.forEach(loan => {
            const daysOverdue = Math.floor((new Date() - new Date(loan.dueDate)) / (1000 * 60 * 60 * 24));

            html += `
                <tr>
                    <td>${loan.loanId}</td>
                    <td>${loan.userId}</td>
                    <td>${loan.bookId}</td>
                    <td>${formatDate(loan.dueDate)}</td>
                    <td><strong>${daysOverdue} dagar</strong></td>
                </tr>
            `;
        });

        html += `
                    </tbody>
                </table>
            </div>
        `;

        adminContent.innerHTML = html;

    } catch (error) {
        console.error('Error loading overdue loans:', error);
        adminContent.innerHTML = '<div class="alert alert-danger">Kunde inte ladda försenade lån</div>';
    }
}

// Ladda admin statistik
async function loadAdminStatistics() {
    try {
        // Hämta statistik från olika endpoints
        const [booksRes, usersRes, loansRes, authorsRes] = await Promise.all([
            authenticatedFetch('/books'),
            authenticatedFetch('/users'),
            authenticatedFetch('/loans'),
            authenticatedFetch('/authors')
        ]);

        if (booksRes) {
            const books = await booksRes.json();
            document.getElementById('bookCount').textContent = books.length;
        }

        if (usersRes) {
            const users = await usersRes.json();
            document.getElementById('userCount').textContent = users.length;
        }

        if (loansRes) {
            const loans = await loansRes.json();
            const activeLoans = loans.filter(l => !l.returnedDate);
            document.getElementById('loanCount').textContent = activeLoans.length;
        }

        if (authorsRes) {
            const authors = await authorsRes.json();
            document.getElementById('authorCount').textContent = authors.length;
        }

    } catch (error) {
        console.error('Error loading statistics:', error);
    }
}