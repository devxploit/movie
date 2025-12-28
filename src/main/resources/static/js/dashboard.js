const API_BASE = '/api/dashboard';

// State
let state = {
    currentTab: 'movies',
    page: 0,
    size: 10,
    search: '',
    sortBy: 'id',
    direction: 'desc'
};

// DOM Elements
const moviesSection = document.getElementById('movies-section');
const seriesSection = document.getElementById('series-section');
const genresSection = document.getElementById('genres-section');
const usersSection = document.getElementById('users-section'); // New
const syncSection = document.getElementById('sync-section');

const moviesTableBody = document.getElementById('movies-table-body');
const seriesTableBody = document.getElementById('series-table-body');
const genresTableBody = document.getElementById('genres-table-body');
const usersTableBody = document.getElementById('users-table-body'); // New

const searchInput = document.getElementById('search-input');
const modalOverlay = document.getElementById('modal-overlay');
const userModalOverlay = document.getElementById('user-modal-overlay'); // New
const movieForm = document.getElementById('movie-form');
const tvShowForm = document.getElementById('tvshow-form');

// Init
document.addEventListener('DOMContentLoaded', () => {
    loadTab('movies');

    // Search Debounce
    let timeout = null;
    searchInput.addEventListener('input', (e) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => {
            state.search = e.target.value;
            state.page = 0;
            loadData();
        }, 500);
    });
});

// Tab Switching
function switchTab(tab) {
    state.currentTab = tab;
    state.page = 0;
    state.search = ''; // Reset search when switching tabs
    searchInput.value = ''; // Clear search input

    // Update active tab styles
    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach(t => {
        if (t.getAttribute('onclick').includes(`'${tab}'`)) t.classList.add('active');
        else t.classList.remove('active');
    });

    moviesSection.classList.remove('active');
    seriesSection.classList.remove('active');
    genresSection.classList.remove('active');
    usersSection.classList.remove('active');
    syncSection.classList.remove('active');

    if (tab === 'movies') moviesSection.classList.add('active');
    else if (tab === 'tvshows') seriesSection.classList.add('active');
    else if (tab === 'genres') genresSection.classList.add('active');
    else if (tab === 'users') usersSection.classList.add('active');
    else if (tab === 'sync') syncSection.classList.add('active');

    loadData();
}

// --- USER FUNCTIONS ---

function openUserModal(userId = null) {
    userModalOverlay.classList.add('active');
    const title = document.getElementById('user-modal-title');
    const idField = document.getElementById('user-id');
    const emailField = document.getElementById('user-email');
    const passField = document.getElementById('user-password');
    const roleField = document.getElementById('user-role');
    const statusField = document.getElementById('user-status');

    if (userId) {
        title.textContent = 'Edit User';
        // Fetch specific user details if needed, or find in current data
        // For simplicity, we just fetch from API to be safe
        fetch(`/api/dashboard/users/${userId}`)
            .then(res => res.json())
            .then(user => {
                idField.value = user.id;
                emailField.value = user.email;
                passField.value = ''; // Don't show password
                roleField.value = user.role;
                statusField.value = user.subscriptionStatus;
            });
    } else {
        title.textContent = 'Add User';
        idField.value = '';
        emailField.value = '';
        passField.value = '';
        roleField.value = 'USER';
        statusField.value = 'INACTIVE';
    }
}

function closeUserModal() {
    userModalOverlay.classList.remove('active');
}

async function saveUser() {
    const id = document.getElementById('user-id').value;
    const email = document.getElementById('user-email').value;
    const password = document.getElementById('user-password').value;
    const role = document.getElementById('user-role').value;
    const status = document.getElementById('user-status').value;

    const user = {
        email,
        role,
        subscriptionStatus: status
    };
    if (password) user.password = password;
    if (id) user.id = parseInt(id);

    // If creating new user, password is required validation?
    if (!id && !password) {
        showToast('Password is required for new users', 'error');
        return;
    }

    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/dashboard/users/${id}` : '/api/dashboard/users';

    try {
        const res = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(user)
        });

        if (res.ok) {
            showToast('User saved successfully');
            closeUserModal();
            loadData();
        } else {
            showToast('Error saving user', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Error saving user', 'error');
    }
}

async function deleteUser(id) {
    if (!confirm('Are you sure you want to delete this user?')) return;

    try {
        const res = await fetch(`/api/dashboard/users/${id}`, { method: 'DELETE' });
        if (res.ok) {
            showToast('User deleted successfully');
            loadData();
        } else {
            showToast('Error deleting user', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Error deleting user', 'error');
    }
}

// Data Loading
async function loadData() {
    if (state.currentTab === 'sync') return;

    const url = `${API_BASE}/${state.currentTab}?page=${state.page}&size=${state.size}&search=${state.search}&sortBy=${state.sortBy}&direction=${state.direction}`;

    try {
        const response = await fetch(url);
        const data = await response.json();
        renderTable(data);
    } catch (error) {
        showToast('Error loading data', 'error');
        console.error(error);
    }
}

// Rendering
function renderTable(data) {
    let tbody;
    if (state.currentTab === 'movies') tbody = moviesTableBody;
    else if (state.currentTab === 'tvshows') tbody = seriesTableBody;
    else if (state.currentTab === 'genres') tbody = genresTableBody;
    else if (state.currentTab === 'users') tbody = usersTableBody;

    tbody.innerHTML = '';

    if (data.content.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center">No results found</td></tr>';
        return;
    }

    data.content.forEach(item => {
        const tr = document.createElement('tr');

        if (state.currentTab === 'genres') {
            tr.innerHTML = `
                <td>${item.id}</td>
                <td>${item.name}</td>
                <td>${item.type || '-'}</td>
            `;
        } else if (state.currentTab === 'users') {
            tr.innerHTML = `
                <td>${item.id}</td>
                <td>${item.email}</td>
                <td><span class="badge ${item.role === 'ADMIN' ? 'badge-blue' : 'badge-green'}">${item.role}</span></td>
                <td><span class="badge ${item.subscriptionStatus === 'ACTIVE' ? 'badge-green' : 'badge-danger'}">${item.subscriptionStatus}</span></td>
                <td>${item.lastLogin ? new Date(item.lastLogin).toLocaleDateString() : '-'}</td>
                <td>
                    <button class="btn btn-primary" onclick="openUserModal(${item.id})" style="padding: 0.3rem 0.8rem; font-size: 0.8rem;">Edit</button>
                    <button class="btn btn-danger" onclick="deleteUser(${item.id})" style="padding: 0.3rem 0.8rem; font-size: 0.8rem;">Delete</button>
                </td>
            `;
        } else {
            tr.innerHTML = `
                <td>${item.id}</td>
                <td>
                    <div style="display:flex; align-items:center; gap:10px;">
                         ${item.posterPath ? `<img src="https://image.tmdb.org/t/p/w92${item.posterPath}" style="width:30px; border-radius:4px;">` : ''}
                        ${item.name}
                    </div>
                </td>
                <td>${item.tmdbId || '-'}</td>
                <td><span class="badge badge-blue">${item.userScore || 0}</span></td>
                <td>${state.currentTab === 'movies' ? (item.date || '-') : (item.firstAirDate || '-')}</td>
                <td>
                    <button class="btn btn-primary" onclick="openEditModal(${item.id})" style="padding: 0.3rem 0.8rem; font-size: 0.8rem;">Edit</button>
                    <button class="btn btn-danger" onclick="deleteItem(${item.id})" style="padding: 0.3rem 0.8rem; font-size: 0.8rem;">Delete</button>
                </td>
            `;
        }
        tbody.appendChild(tr);
    });

    renderPagination(data);
}

function renderPagination(data) {
    let containerId;
    if (state.currentTab === 'movies') containerId = 'movies-pagination';
    else if (state.currentTab === 'tvshows') containerId = 'series-pagination';
    else if (state.currentTab === 'genres') containerId = 'genres-pagination';
    else if (state.currentTab === 'users') containerId = 'users-pagination';

    const container = document.getElementById(containerId);
    container.innerHTML = `
        <button class="page-btn" ${data.first ? 'disabled' : ''} onclick="changePage(${state.page - 1})">Prev</button>
        <span style="color:var(--text-secondary); padding:0.5rem;">Page ${data.number + 1} of ${data.totalPages}</span>
        <button class="page-btn" ${data.last ? 'disabled' : ''} onclick="changePage(${state.page + 1})">Next</button>
    `;
}

// ... unchanged ...

// Sync
async function triggerSync() {
    const btn = document.getElementById('sync-btn');
    btn.disabled = true;
    btn.innerText = 'Syncing...';

    try {
        const response = await fetch(`${API_BASE}/sync/first-data`, { method: 'POST' });
        if (response.ok) {
            showToast('Sync triggered successfully', 'success');
        } else {
            showToast('Error triggering sync', 'error');
        }
    } catch (error) {
        showToast('Error triggering sync', 'error');
    } finally {
        setTimeout(() => {
            btn.disabled = false;
            btn.innerText = 'Run First Data Sync';
        }, 5000);
    }
}

async function triggerUpdateUrls() {
    const btn = document.getElementById('update-urls-btn');
    btn.disabled = true;
    btn.innerText = 'Updating...';

    try {
        const response = await fetch(`${API_BASE}/sync/update-urls`, { method: 'POST' });
        const msg = await response.text();
        if (response.ok) {
            showToast(msg, 'success');
        } else {
            showToast('Error: ' + msg, 'error');
        }
    } catch (error) {
        showToast('Error triggering update', 'error');
    } finally {
        setTimeout(() => {
            btn.disabled = false;
            btn.innerText = 'Update New URLs';
        }, 2000);
    }
}

// Utils
function loadTab(tab) {
    switchTab(tab);
}

function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerText = message;
    container.appendChild(toast);

    setTimeout(() => {
        toast.remove();
    }, 3000);
}
