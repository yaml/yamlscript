$Source = @'
using System;
using System.Runtime.InteropServices;

public static class YAMLScriptNative {
  [DllImport("libys")]
  public static extern int graal_create_isolate(
    IntPtr parameters,
    IntPtr isolate,
    ref IntPtr thread);

  [DllImport("libys")]
  public static extern int graal_tear_down_isolate(IntPtr thread);

  [DllImport("libys")]
  public static extern IntPtr load_ys_to_json(IntPtr thread, string input);
}
'@

if (-not ('YAMLScriptNative' -as [type])) {
  Add-Type -TypeDefinition $Source
}

function Invoke-YAMLScriptJson {
  param(
    [Parameter(Mandatory, ValueFromPipeline)]
    [string] $InputObject
  )

  process {
    $thread = [IntPtr]::Zero
    $rc = [YAMLScriptNative]::graal_create_isolate(
      [IntPtr]::Zero,
      [IntPtr]::Zero,
      [ref] $thread)
    if ($rc -ne 0 -or $thread -eq [IntPtr]::Zero) {
      throw 'Failed to create isolate'
    }

    $ptr = [YAMLScriptNative]::load_ys_to_json($thread, $InputObject)
    if ($ptr -eq [IntPtr]::Zero) {
      [void] [YAMLScriptNative]::graal_tear_down_isolate($thread)
      throw 'Null response from libys'
    }

    $json = [Runtime.InteropServices.Marshal]::PtrToStringAnsi($ptr)
    $rc = [YAMLScriptNative]::graal_tear_down_isolate($thread)
    if ($rc -ne 0) {
      throw 'Failed to tear down isolate'
    }
    $json
  }
}

function Invoke-YAMLScript {
  param(
    [Parameter(Mandatory, ValueFromPipeline)]
    [string] $InputObject
  )

  process {
    $response = Invoke-YAMLScriptJson $InputObject | ConvertFrom-Json
    if ($null -ne $response.error) {
      throw $response.error.cause
    }
    $response.data
  }
}

Export-ModuleMember -Function Invoke-YAMLScript, Invoke-YAMLScriptJson
