'
' Calculate year-average population
'      
'      pstart = population at the start of the year (people or thouseands or millions)
'      cbr = cruide birth rate (promille)
'      cdr = cruide death rate (promille)
'      migr = migration balances (same units as pstart)
'

Public Function YearAveragePopulation( _
    ByVal pstart As Variant, _
    ByVal cbr As Variant, _
    ByVal cdr As Variant, _
    ByVal migr As Variant) As Variant

    On Error GoTo BadValue

    Dim ps As Double
    Dim br As Double
    Dim dr As Double
    Dim mig As Double

    Dim r As Double
    Dim A As Double
    Dim B As Double

    If Not ToDouble(pstart, ps) Then GoTo BadValue
    If Not ToDouble(cbr, br) Then GoTo BadValue
    If Not ToDouble(cdr, dr) Then GoTo BadValue
    If Not ToDouble(migr, mig) Then GoTo BadValue

    r = (br - dr) / 1000#

    If Abs(r) < 0.00000001 Then
        A = 1# + r / 2# + r ^ 2 / 6# + r ^ 3 / 24# + r ^ 4 / 120#
        B = 1# / 2# + r / 6# + r ^ 2 / 24# + r ^ 3 / 120# + r ^ 4 / 720#
    Else
        A = (Exp(r) - 1#) / r
        B = (A - 1#) / r
    End If

    YearAveragePopulation = ps * A + mig * B
    Exit Function

BadValue:
    YearAveragePopulation = CVErr(xlErrValue)

End Function


Private Function ToDouble(ByVal v As Variant, ByRef out As Double) As Boolean

    On Error GoTo BadValue

    Dim s As String
    Dim decSep As String
    Dim thouSep As String

    If IsError(v) Then GoTo BadValue

    If IsEmpty(v) Then
        out = 0#
        ToDouble = True
        Exit Function
    End If

    If IsNumeric(v) Then
        out = CDbl(v)
        ToDouble = True
        Exit Function
    End If

    s = CStr(v)
    s = Trim$(s)

    If Len(s) = 0 Then
        out = 0#
        ToDouble = True
        Exit Function
    End If

    s = Replace(s, Chr$(160), "")
    s = Replace(s, " ", "")

    decSep = Application.International(xlDecimalSeparator)
    thouSep = Application.International(xlThousandsSeparator)

    If Len(thouSep) > 0 Then
        s = Replace(s, thouSep, "")
    End If

    If decSep = "," Then
        If InStr(s, ",") = 0 And InStr(s, ".") > 0 Then
            s = Replace(s, ".", ",")
        End If
    Else
        If InStr(s, ".") = 0 And InStr(s, ",") > 0 Then
            s = Replace(s, ",", ".")
        End If
    End If

    If Not IsNumeric(s) Then GoTo BadValue

    out = CDbl(s)
    ToDouble = True
    Exit Function

BadValue:
    ToDouble = False

End Function


