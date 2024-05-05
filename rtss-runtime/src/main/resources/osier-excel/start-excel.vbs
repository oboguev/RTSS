Function say(text)
    WScript.StdOut.WriteLine text & vbCrLf
End Function

Set fso = CreateObject ("Scripting.FileSystemObject")
Set Stdout = fso.GetStandardStream (1)
Set Stderr = fso.GetStandardStream (2)

dim nl
nl = Chr(10)

dim app
set app = createobject("Excel.Application")
app.Visible = true
app.UserControl = false

dim wb
set wb = app.workbooks.add

' When Excel is started via automation, it does not activate addins
' Force-reactivate ExcelOSR add-in
' https://stackoverflow.com/questions/25114702/add-ins-not-loading-when-opening-excel-file-programmatically

dim CurrAddin
dim s

for Each CurrAddin In app.AddIns
    's = "False"
    'if  CurrAddin.Installed then
    '    s = "True"
    'end if
    'Stdout.Write "AddIn: " + CurrAddin.Name + ", Installed: " + s + nl
    if CurrAddin.Name = "ExcelOSR.xla" then
        Stdout.Write "Reactivating ExcelOSR" + nl
        CurrAddin.Installed = False
        CurrAddin.Installed = True
    end if
Next

' range, for later use
dim rng

say "Start script has completed"

'---execute---
