#include "stdafx.h"

static HMODULE hXLCall32 = NULL;
HMODULE hOsierXLL = NULL;
map<string, XllFunction> xllFunctions;

void load_libraries();
int pascal MyExcel4v(int xlfn, LPXLOPER r, int count, LPXLOPER op[]);

int _tmain(int argc, _TCHAR* argv[])
{
    load_libraries();
	return 0;
}

/* ========================================================================================= */

static void load_libraries()
{
	TCHAR szFileName[MAX_PATH];
	GetModuleFileName(NULL, szFileName, MAX_PATH);
	TCHAR* pFile = _tcsrchr(szFileName, '\\') + 1;

	_tcscpy(pFile, _T("XLCall32.dll"));
	hXLCall32 = LoadLibrary(szFileName);
	if (hXLCall32 == NULL)
		fatal(xprintf(_T("Unable to load %s"), szFileName));

	p_setExcel4vInterceptor_t p_setExcel4vInterceptor = (p_setExcel4vInterceptor_t)GetProcAddress(hXLCall32, "setExcel4vInterceptor");
	if (p_setExcel4vInterceptor == NULL)
		fatal(xprintf(_T("Unable to load %s"), szFileName));
	(*p_setExcel4vInterceptor)(MyExcel4v);

	_tcscpy(pFile, _T("Osier.xll"));
	hOsierXLL = LoadLibrary(szFileName);
	if (hOsierXLL == NULL)
		fatal(xprintf(_T("Unable to load %s"), szFileName));

	p_i_v_t pxlAutoOpen = (p_i_v_t)GetProcAddress(hOsierXLL, "xlAutoOpen");
	if (pxlAutoOpen == NULL)
		fatal("Unable to access xlAutoOpen");
	(*pxlAutoOpen)();

	noop();
}

/* ========================================================================================= */

/*
 * For XLL API see https://www.codeproject.com/Articles/5263/Sample-Excel-Function-Add-in-in-C
 */

static int pascal MyExcel4v(int xlfn, LPXLOPER r, int count, LPXLOPER op[])
{
	if (xlfn == xlGetName && count == 0)
	{
		char path[_MAX_PATH];
		if (0 == GetModuleFileNameA(hOsierXLL, path, _MAX_PATH))
			fatal("Unable to get the path of Osier XLL");
		r->xltype = xltypeStr;
		r->val.str = to_counted_string(path);

		return xlretSuccess;
	}
	else if (xlfn == xlfRegister && count >= 2)
	{
		// op[0] is DLL path
		// op[1] is the name of exported DLL routine for the function
		// op[2] is argument types such as "RRRRR"
		// op[3] is public function name
		// op[4] is argument name list such as "MortHandle,Age,[AgeInterval],[Cause]"
		// op[5] is whether function returns values (1 = yes, 2 = void)
		// op[6] is category name
		// op[7] is hotkey assignment
		// op[8] is filename and help ID for this function
		// op[9] is a textual description of the function itself
		// op[10...] is textual description for every argument

		// dump call arguments
		string s;
		for (int k = 1; k < count; k++)
		{
			if (op[k]->xltype != xltypeStr)
				fatal("xlfRegister: not a string operand");
			if (s.length() != 0)
				s += "|";
			s += from_counted_string(op[k]->val.str);
		}
		// fprintf(stderr, "%s\n", s.c_str());
		fflush(stderr);

		if (count < 10)
			fatal("Insufficient number of argument for xlfRegister");

		XllFunction f;
		f.dllname = op2string(op[1]);
		f.dllname = op2string(op[3]);
		f.comment = op2string(op[9]);

		s = op2string(op[5]);
		if (s == "1")
			f.is_void = false;
		else if (s == "2")
			f.is_void = true;
		else
			fatal("Unexpected function definition (isvoid)");
		
		string argtypes = op2string(op[2]);
		string argnames = op2string(op[4]);

		return xlretSuccess;
	}
	else if (xlfn == xlFree)
	{
		for (int k = 0; k < count; k++)
		{
			if (op[k]->xltype == xltypeStr)
			{
				if (op[k]->val.str != NULL)
				{
					// what kind of free?
					op[k]->val.str = NULL;
				}
			}
			else if (op[k]->xltype == xltypeRef)
			{
				// do nothing
				op[k]->val.mref.lpmref = NULL;
			}
			else
			{
				// fatal("xlFree: not a string operand and not ref");
			}
		}

		return xlretSuccess;
	}
	else
	{
		fatal(xprintf("Unknown Excel function %d\n", xlfn));
	}

	return xlretSuccess;
}
