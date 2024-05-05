if wb != Nothing then
    wb.Close(false)
    set wb = Nothing
end if

'---execute---

if app != Nothing then
    app.Quit
    set app = Nothing
end if

'---execute---
