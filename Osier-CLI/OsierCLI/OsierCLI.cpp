#include "stdafx.h"

void load_libraries();
void fatal(const TCHAR* msg);
void fatal(const char* msg);
TCHAR* xprintf(const TCHAR* format, ...);
std::string GetLastErrorAsString(void);

typedef int (WINAPI *p_i_v_t)(void);

static void noop()
{
}

static string tchar2string(TCHAR* t)
{
	string str = t; 
	return str;
}

char* to_counted_string(const char* s)
{
	char* p = (char*) malloc(strlen(s) + 2);
	strcpy(p + 1, s);
	p[0] = (char) strlen(p + 1);
	return p;
}

string from_counted_string(const char* s)
{
	string xs;
	int len = (unsigned char) *s++;
	for (int k = 0; k < len; k++)
		xs += *s++;
	return xs;
}


static HMODULE hXLCall32 = NULL;
static HMODULE hOsierXLL = NULL;

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

static void fatal(const char* msg)
{
	fprintf(stderr, msg);
	fprintf(stderr, "\n");

	fprintf(stderr, GetLastErrorAsString().c_str());
	fprintf(stderr, "\n");

	exit(1);
}

static TCHAR* xprintf(const TCHAR* format, ...)
{
	va_list args;
	va_start(args, format); 

	TCHAR* result = NULL;
	size_t count = 300;

	for (;;)
	{
		if (result != NULL)
			free(result);
		result = (TCHAR*) calloc(count, sizeof(TCHAR));
		if (result == NULL)
			fatal(_T("Out of memory"));

		int r = _vsntprintf(result, count, format, args);
		if (result < 0)
			fatal(_T("Formatting error"));
		if (r < (int) count)
		{
			va_end(args);
			return result;
		}
		else
		{
			count *= 2;
			if (r > (int) count)
				count = r + 1;
		}
	}

	return result;
}

static string GetLastErrorAsString(void)
{
	//Get the error message ID, if any.
	DWORD errorMessageID = ::GetLastError();
	if (errorMessageID == 0) {
		return string(); //No error message has been recorded
	}

	LPSTR messageBuffer = nullptr;

	//Ask Win32 to give us the string version of that message ID.
	//The parameters we pass in, tell Win32 to create the buffer that holds the message for us (because we don't yet know how long the message string will be).
	size_t size = FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
		NULL, errorMessageID, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), (LPSTR)&messageBuffer, 0, NULL);

	//Copy the error message into a string.
	string message(messageBuffer, size);

	//Free the Win32's string's buffer.
	LocalFree(messageBuffer);

	return message;
}