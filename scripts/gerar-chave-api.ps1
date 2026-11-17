# Execute localmente e coloque o resultado na variável API_JWT_SECRET; não versione a chave.
$chaveApi = New-Object byte[] 32
$geradorApi = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $geradorApi.GetBytes($chaveApi)
    [Convert]::ToBase64String($chaveApi)
} finally {
    $geradorApi.Dispose()
    [Array]::Clear($chaveApi, 0, $chaveApi.Length)
}
