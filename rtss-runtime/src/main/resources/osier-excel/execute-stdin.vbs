'
' Execute cscript command stream from StdIn
'
' Input is a stream of executable sections of code.
' Each section is terminated with pseudo-comment line "`---execute---".
'
' Each section is buffered until it is gathered in its entirety, and then is executed.
'

Dim StdIn, StdOut
Set StdIn = WScript.StdIn
Set StdOut = WScript.StdOut

dim buffer
buffer = ""

dim wb
set wb = Nothing

dim app
set app = Nothing

Do While Not StdIn.AtEndOfStream

     str = StdIn.ReadLine
     'StdOut.WriteLine ">>> " & str

     if InStr(1, str, "'---execute---") = 1 then
         executeGlobal buffer
	 buffer = ""
     elseif buffer = "" then
         buffer = str
     else
         buffer = buffer & vbCrLf & str
     end if

Loop

' close wb and app on end-of-input, if they were not closed yet

if wb Is Nothing then
else
    wb.Close(false)
    set wb = Nothing
end if

If app Is Nothing Then
else
    app.Quit
    set app = Nothing
End If
