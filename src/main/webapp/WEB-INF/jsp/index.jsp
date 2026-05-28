<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Analytics Dashboard</title>

    <!-- Bootstrap -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">

    <!-- Chart.js -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

    <style>
        body {
            background: #f4f6f9;
        }
        .card {
            border-radius: 12px;
        }
        .stat-card {
            text-align: center;
            padding: 20px;
        }
        .stat-value {
            font-size: 28px;
            font-weight: bold;
        }
        .navbar {
            background: #1f2937;
        }
        .navbar-brand {
            color: #fff !important;
        }
    </style>
</head>

<body>

<!-- 🔹 Navbar -->
<nav class="navbar navbar-expand-lg">
    <div class="container-fluid">
        <span class="navbar-brand">Analytics Dashboard</span>
        <div class="ms-auto">
            <a href="/login" class="btn btn-sm btn-light">Logout</a>
        </div>
    </div>
</nav>

<div class="container mt-4">

    <!-- 🔹 Stats -->
    <div class="row g-3">
        <div class="col-md-3">
            <div class="card stat-card shadow-sm">
                <div>Total Users</div>
                <div class="stat-value text-primary" id="totalUsers">0</div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card stat-card shadow-sm">
                <div>Active Sessions</div>
                <div class="stat-value text-success" id="activeSessions">0</div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card stat-card shadow-sm">
                <div>Requests Today</div>
                <div class="stat-value text-warning" id="requestsToday">0</div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card stat-card shadow-sm">
                <div>Errors</div>
                <div class="stat-value text-danger" id="errors">0</div>
            </div>
        </div>
    </div>

    <!-- 🔹 Charts -->
    <div class="row mt-4">
        <div class="col-md-6">
            <div class="card p-3 shadow-sm">
                <h6>User Growth</h6>
                <canvas id="userChart"></canvas>
            </div>
        </div>

        <div class="col-md-6">
            <div class="card p-3 shadow-sm">
                <h6>Requests Overview</h6>
                <canvas id="requestChart"></canvas>
            </div>
        </div>
    </div>

</div>

<script>
    // 🔹 Sample Data (replace with API)
    document.getElementById('totalUsers').innerText = 1250;
    document.getElementById('activeSessions').innerText = 87;
    document.getElementById('requestsToday').innerText = 430;
    document.getElementById('errors').innerText = 5;

    // 🔹 User Chart
    new Chart(document.getElementById('userChart'), {
        type: 'line',
        data: {
            labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May'],
            datasets: [{
                label: 'Users',
                data: [200, 400, 600, 900, 1250]
            }]
        }
    });

    // 🔹 Requests Chart
    new Chart(document.getElementById('requestChart'), {
        type: 'bar',
        data: {
            labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'],
            datasets: [{
                label: 'Requests',
                data: [50, 120, 180, 90, 220]
            }]
        }
    });
</script>

</body>
</html>