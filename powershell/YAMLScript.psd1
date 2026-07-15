@{
  RootModule = 'YAMLScript.psm1'
  ModuleVersion = '0.2.27'
  GUID = '3169069f-8994-44b9-b086-8384419f45c4'
  Author = 'YAMLScript Contributors'
  CompanyName = 'YAMLScript'
  Copyright = '(c) 2026 YAMLScript Contributors'
  Description = 'YAMLScript language binding for PowerShell'
  PowerShellVersion = '7.0'
  FunctionsToExport = @('Invoke-YAMLScript', 'Invoke-YAMLScriptJson')
  CmdletsToExport = @()
  VariablesToExport = '*'
  AliasesToExport = @()
  PrivateData = @{
    PSData = @{
      Tags = @('yaml', 'yamlscript')
      LicenseUri = 'https://github.com/yaml/yamlscript/blob/main/License'
      ProjectUri = 'https://github.com/yaml/yamlscript'
    }
  }
}
