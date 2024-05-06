' range, for later use
dim rng

Function say(text)
    WScript.StdOut.WriteLine text ' & vbCrLf
End Function

Function setCellText(ca, value)
    dim rng2
    set rng2 = wb.Activesheet.Range(ca)
    [rng2].Value = value
End Function

Function setCellFormula(ca, value)
    dim rng2
    set rng2 = wb.Activesheet.Range(ca)
    [rng2].Value = value
End Function

Function setCellInt(ca, value)
    dim rng2
    set rng2 = wb.Activesheet.Range(ca)
    [rng2].Value = value
    [rng2].NumberFormat = "0"
End Function

Function setCellDouble(ca, value)
    dim rng2
    set rng2 = wb.Activesheet.Range(ca)
    [rng2].Value = value
    [rng2].NumberFormat = "0.0000"
End Function

Function showCellValue(what, ca)
    dim rng2
    set rng2 = wb.Activesheet.Range(ca)
    say what & CStr([rng2].Value)
End Function

Set fso = CreateObject ("Scripting.FileSystemObject")
Set Stdout = fso.GetStandardStream (1)
Set Stderr = fso.GetStandardStream (2)

dim nl
nl = Chr(10)

dim app
set app = createobject("Excel.Application")
app.Visible = ${visible}
app.UserControl = false

dim wb
set wb = app.workbooks.add

' When Excel is started via automation, it does not activate addins
' Force-reactivate ExcelOSR add-in
' https://stackoverflow.com/questions/25114702/add-ins-not-loading-when-opening-excel-file-programmatically

dim CurrAddin
dim s

for Each CurrAddin In app.AddIns
    say "Addins: " & CurrAddin.Path & " file " & CurrAddin.Name 
    's = "False"
    'if  CurrAddin.Installed then
    '    s = "True"
    'end if
    'Stdout.Write "AddIn: " + CurrAddin.Name + ", Installed: " + s + nl
    if CurrAddin.Name = "ExcelOSR.xla" then
        Stdout.Write "Reactivating " & CurrAddin.Name & " at " & CurrAddin.Path & nl
        CurrAddin.Installed = False
        CurrAddin.Installed = True
    end if
Next

'---execute---

for Each CurrAddin In app.AddIns2
    say "Addins2: " & CurrAddin.Path & " file " & CurrAddin.Name 
    if CurrAddin.Name = "ExcelOSR.xla" Or CurrAddin.Name = "OSR.xla" then
        Stdout.Write "Reactivating " & CurrAddin.Name & " at " & CurrAddin.Path & nl
        CurrAddin.Installed = False
        CurrAddin.Installed = True
    end if
Next

'---execute---

say "Start script has completed"

'---execute---
