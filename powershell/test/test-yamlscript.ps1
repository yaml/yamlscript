Import-Module "$PSScriptRoot/../YAMLScript.psd1" -Force

$data = Invoke-YAMLScript "!ys-0:`ntest:: inc(41)"

if ($data.test -ne 42) {
  throw 'load ys code failed'
}

Write-Output 'ok - load ys code'
