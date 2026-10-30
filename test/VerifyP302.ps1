param([string]$BaseUrl = 'http://127.0.0.1:18080/KeebForce')

# HTTP thật qua Tomcat; dùng tài khoản/catalog demo đã bàn giao, không tạo đơn.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$script:checks = 0
$clients = @()

function Check([bool]$condition, [string]$message) {
    if (!$condition) { throw $message }
    $script:checks++
    Write-Output "OK $script:checks $message"
}

function New-Client {
    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.AllowAutoRedirect = $false
    $handler.CookieContainer = New-Object System.Net.CookieContainer
    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(15)
    return $client
}

function Send($client, [string]$path, [hashtable]$data) {
    if ($null -eq $data) {
        $response = $client.GetAsync("$BaseUrl$path").GetAwaiter().GetResult()
    } else {
        $pairs = New-Object 'System.Collections.Generic.Dictionary[string,string]'
        foreach ($key in $data.Keys) { $pairs.Add($key, [string]$data[$key]) }
        $content = New-Object System.Net.Http.FormUrlEncodedContent($pairs)
        try { $response = $client.PostAsync("$BaseUrl$path", $content).GetAwaiter().GetResult() }
        finally { $content.Dispose() }
    }
    try {
        return [pscustomobject]@{
            Status = [int]$response.StatusCode
            Html = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            Location = [string]$response.Headers.Location
        }
    } finally { $response.Dispose() }
}

function Token([string]$html) {
    $match = [regex]::Match($html, 'name="csrf"\s+value="([^"]+)"')
    if (!$match.Success) { throw 'CSRF token missing' }
    return [Net.WebUtility]::HtmlDecode($match.Groups[1].Value)
}

function Login($client, [string]$username, [string]$password) {
    $page = Send $client '/login' $null
    $result = Send $client '/login' @{ username=$username; password=$password; csrf=(Token $page.Html) }
    Check ($result.Status -eq 302 -and $result.Location.EndsWith('/profile')) "Login $username"
}

try {
    $anonymous = New-Client
    $customer = New-Client
    $admin = New-Client
    $clients = @($anonymous, $customer, $admin)
    $result = Send $anonymous '/request?serviceId=1' $null
    Check ($result.Status -eq 302 -and $result.Location.EndsWith('/login')) 'Anonymous redirected to login'
    Login $customer 'customer1' 'Customer@123'
    Login $admin 'admin' 'Admin@123'
    Check ((Send $admin '/request?serviceId=1' $null).Status -eq 403) 'ADMIN cannot open customer form'
    $page = Send $customer '/request?serviceId=1' $null
    Check ($page.Status -eq 200 -and $page.Html.Contains('Lắp ráp bàn phím')) 'Active service from real DB'
    Check ($page.Html -match '200\.000\s*₫') 'Published service price'
    Check (([regex]::Matches($page.Html, 'name="productIds"')).Count -eq 10) '10 active products from P2 DAO'
    Check ($page.Html -match 'name="contactName"[\s\S]*?value="[^"]+"' -and
        $page.Html -match 'name="contactPhone"') 'Contact fields populated from session account'
    Check ($page.Html.Contains('name="serviceId" value="1"') -and $page.Html.Contains('name="action" value="create"') -and
        (Token $page.Html).Length -gt 20) 'Form contract and common CSRF token'
    Check ($page.Html -match 'id="deviceDescription"[^>]+required' -and
        $page.Html -match 'id="customerNote"[^>]+maxlength="1000"') 'Device required and note length limited'
    Check ($page.Html -match 'name="quantity_1" min="1" max="200" step="1"') 'Per-product quantity contract'
    $today = [TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTime]::UtcNow, 'SE Asia Standard Time').ToString('yyyy-MM-dd')
    Check ($page.Html.Contains("min=`"$today`"")) 'Preferred date starts today in Vietnam'
    Check ($page.Html -match 'type="submit"[^>]+disabled' -and $page.Html.Contains('Tổng ước tính')) 'Submission disabled at GET-only step'
    Check (([regex]::Matches($page.Html, '<header\b')).Count -eq 1 -and
        ([regex]::Matches($page.Html, '<main\b')).Count -eq 1 -and
        ([regex]::Matches($page.Html, '<footer\b')).Count -eq 1 -and $page.Html.Contains('Assignment')) 'Shared layout and footer'
    foreach ($query in @('', '?serviceId=', '?serviceId=abc', '?serviceId=0', '?serviceId=-1',
            '?serviceId=2147483648', '?serviceId=1&serviceId=2', '?serviceId=%3Cscript%3E', '?serviceId=1.5')) {
        $bad = Send $customer "/request$query" $null
        Check ($bad.Status -eq 400 -and !$bad.Html.Contains('id="request-form"')) "Bad service query rejected: $query"
    }
    $missing = Send $customer '/request?serviceId=2147483647' $null
    Check ($missing.Status -eq 404 -and !$missing.Html.Contains('id="request-form"')) 'Missing service has no form'
    # Đổi trạng thái qua API Admin hiện có rồi luôn phục hồi trạng thái ban đầu.
    $adminPage = Send $admin '/admin/services?id=1' $null
    if ($adminPage.Html -notmatch '<option value="true" selected>') {
        throw 'Expected seed service #1 to be active; refusing to change an unknown state'
    }
    $csrf = Token $adminPage.Html
    try {
        $toggle = Send $admin '/admin/services' @{action='toggle'; id='1'; active='false'; csrf=$csrf}
        Check ($toggle.Status -eq 302) 'Temporarily stop accepting service'
        $inactive = Send $customer '/request?serviceId=1' $null
        Check ($inactive.Status -eq 404 -and !$inactive.Html.Contains('id="request-form"')) 'Inactive service rejected'
    } finally {
        $restored = Send $admin '/admin/services' @{action='toggle'; id='1'; active='true'; csrf=$csrf}
        if ($restored.Status -ne 302) { throw 'Could not restore service #1 active state' }
    }
    Check ((Send $customer '/request?serviceId=1' $null).Status -eq 200) 'Service restored after test'
    Check ((Send $customer '/WEB-INF/views/request-form.jsp' $null).Status -eq 404) 'JSP protected under WEB-INF'
    foreach ($asset in @('/css/orders.css','/js/orders.js')) {
        Check ((Send $anonymous $asset $null).Status -eq 200) "Public asset $asset"
    }
    Check ((Send $customer '/request' @{action='create'; serviceId='1'}).Status -eq 403) 'POST without CSRF rejected by P1 Filter'
    Check ((Send $customer '/request' @{action='create'; serviceId='1'; csrf=(Token $page.Html)}).Status -eq 405) 'POST not implemented before P3-03'
    $spoof = Send $customer '/request?serviceId=1&accountId=1&role=ADMIN&totalAmount=1&contactName=FAKE' $null
    Check ($spoof.Status -eq 200 -and !$spoof.Html.Contains('value="FAKE"')) 'GET does not accept account or price spoofing'
    Write-Output "PASS $script:checks HTTP checks. No order created."
} finally {
    foreach ($client in $clients) { $client.Dispose() }
}
