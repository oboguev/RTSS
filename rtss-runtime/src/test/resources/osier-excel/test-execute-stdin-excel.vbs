Set fso = CreateObject ("Scripting.FileSystemObject")
Set Stdout = fso.GetStandardStream (1)
Set Stderr = fso.GetStandardStream (2)

dim nl
nl = Chr(10)

'WScript.StdOut.WriteLine "Hello2"
'Stdout.WriteLine "Hello3"
'Stdout.Write "Hello4"

dim app
set app = createobject("Excel.Application")
app.Visible = true
app.UserControl = false

dim wb
set wb = app.workbooks.add

set rng = wb.Activesheet.Range("A1")
[rng].Value = "aaa-bbb-ccc-zzz"

set rng = wb.Activesheet.Range("A2")
[rng].Value = "1"
[rng].NumberFormat = "0"

set rng = wb.Activesheet.Range("A3")
[rng].Value = "1.95"
[rng].NumberFormat = "0.0000"
Stdout.Write "A3=" + CStr([rng].Value) + nl

set rng = wb.Activesheet.Range("A4")
[rng].Formula = "=A2+A3"
Stdout.Write "A4=" + CStr([rng].Value) + nl

[rng].EntireColumn.AutoFit
[rng].Calculate

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

'---execute---

'Stdout.Write "Sleeping" + nl
'WScript.Sleep 5000
'Stdout.Write "Waking" + nl

wb.Close(false)
set wb = Nothing
'WScript.Sleep 1000
'app.Quit
set app = Nothing

'app.UserControl = true

'---execute---
