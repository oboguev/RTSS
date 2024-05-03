#include "stdafx.h"

void load_libraries();

static HMODULE hXLCall32 = NULL;
HMODULE hOsierXLL = NULL;

int _tmain(int argc, _TCHAR* argv[])
{
    load_libraries();
	return 0;
}

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
		// op[0] is DLL
		// op[1] is function name
		string s;
		for (int k = 1; k < count; k++)
		{
			if (op[k]->xltype != xltypeStr)
				fatal("xlfRegister: not a string operand");
			if (s.length() != 0)
				s += "|";
			s += from_counted_string(op[k]->val.str);
		}
		fprintf(stderr, "%s\n", s.c_str());
		fflush(stderr);
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
		fprintf(stderr, "Unknown Excel function %d\n", xlfn);
	}

	return xlretSuccess;
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

	p_setExcel4vInterceptor_t p_setExcel4vInterceptor = (p_setExcel4vInterceptor_t) GetProcAddress(hXLCall32, "setExcel4vInterceptor");
	if (p_setExcel4vInterceptor == NULL)
		fatal(xprintf(_T("Unable to load %s"), szFileName));
    (*p_setExcel4vInterceptor)(MyExcel4v);

	_tcscpy(pFile, _T("Osier.xll"));
	hOsierXLL = LoadLibrary(szFileName);
	if (hOsierXLL == NULL)
		fatal(xprintf(_T("Unable to load %s"), szFileName));

	p_i_v_t pxlAutoOpen = (p_i_v_t) GetProcAddress(hOsierXLL, "xlAutoOpen");
	if (pxlAutoOpen == NULL)
		fatal("Unable to access xlAutoOpen");
	(*pxlAutoOpen)();

	noop();
}

