Osier-CLI is used to call Osier library functions.
These functions interpolate mortality curves and perform other demographic related math computations.
RTSS invokes Osier library functions through OsierCLI executable.

Visual Studio solution Osier-CLI contains two components:
- OsierCLI excutable (OsierCLI.EXE)
- XLCall32 DLL (XLCall32.DLL)

Download Excel SDK
https://www.microsoft.com/en-us/download/details.aspx?id=35567
and install it into C:\WINAPPS\Excel-SDK

Open Osier-CLI.sln in Visual Studio 2019 (16.5.4) or later.

Select platform = x64

Build.

Download attachment to article
    Sigurd Dyrting, Andrew Taylor, "Estimating age-specific mortality using calibrated splines"
    https://doi.org/10.1080/00324728.2023.2228297
Attachment location: https://doi.org/10.17605/OSF.IO/NGR8X (file ESM_2.zip).
Unpack included Osier00_x64_20220524.zip into Osier installation directory, e.g. C:\WINAPPS\Osier.

Copy x64/OsierCLI.EXE and x64/XLCall32.DLL to directory C:\WINAPPS\Osier\ExcelOSR\Addin.

Change file rtss-config.yml, parameter Osier.executable to point to [Addin directory]\OsierCLI.

OsierCLI must run on a Windows machine.
It can also be hosted on a machine different than used to execute RTSS itself.
To execute Osier remotely from RTSS, configure Osier.server.endpoint in rtss-config.yml and run RTSS server on the machine hosting Osier.
RTSS will invoke RTSS server that will invoke Osier.