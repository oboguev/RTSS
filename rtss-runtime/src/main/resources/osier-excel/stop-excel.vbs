if wb Is Nothing then
else
    wb.Close(false)
    set wb = Nothing
end if

'---execute---

If app Is Nothing Then
else
    app.Quit
    set app = Nothing
End If

'---execute---
