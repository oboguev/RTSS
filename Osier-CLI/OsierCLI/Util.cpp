#include "stdafx.h"

void fatal(const char* msg)
{
	fprintf(stderr, msg);
	fprintf(stderr, "\n");

	fprintf(stderr, GetLastErrorAsString().c_str());
	fprintf(stderr, "\n");

	exit(1);
}

TCHAR* xprintf(const TCHAR* format, ...)
{
	va_list args;
	va_start(args, format);

	TCHAR* result = NULL;
	size_t count = 300;

	for (;;)
	{
		if (result != NULL)
			free(result);
		result = (TCHAR*)calloc(count, sizeof(TCHAR));
		if (result == NULL)
			fatal(_T("Out of memory"));

		int r = _vsntprintf(result, count, format, args);
		if (result < 0)
			fatal(_T("Formatting error"));
		if (r < (int)count)
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
	DWORD errorMessageID = ::GetLastError();
	if (errorMessageID == 0)
return string(); //No error message has been recorded

LPSTR messageBuffer = nullptr;

size_t size = FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
	NULL, errorMessageID, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), (LPSTR)&messageBuffer, 0, NULL);

string message(messageBuffer, size);
LocalFree(messageBuffer);
return message;
}

string tchar2string(TCHAR* t)
{
	string str = t;
	return str;
}

char* to_counted_string(const char* s)
{
	char* p = (char*)malloc(strlen(s) + 2);
	strcpy(p + 1, s);
	p[0] = (char)strlen(p + 1);
	return p;
}

string from_counted_string(const char* s)
{
	string xs;
	int len = (unsigned char)*s++;
	for (int k = 0; k < len; k++)
		xs += *s++;
	return xs;
}

string op2string(const XLOPER* x)
{
	if (x->xltype != xltypeStr)
		fatal("Unexpected: XLOPER is not a string");
	string s = from_counted_string(x->val.str);
	return s;
}

vector<string> split(const string& str, const string& delim)
{
	vector<string> result;
	size_t start = 0;

	for (size_t found = str.find(delim); found != string::npos; found = str.find(delim, start))
	{
		result.emplace_back(str.begin() + start, str.begin() + found);
		start = found + delim.size();
	}

	if (start != str.size())
		result.emplace_back(str.begin() + start, str.end());

	return result;
}

// trim from start (in place)
void ltrim(string& s)
{
	s.erase(s.begin(), std::find_if(s.begin(), s.end(), [](unsigned char ch)
		{
			return !std::isspace(ch);
		}));
}

// trim from end (in place)
void rtrim(string& s)
{
	s.erase(std::find_if(s.rbegin(), s.rend(), [](unsigned char ch)
		{
			return !std::isspace(ch);
		}).base(), s.end());
}

// trim from both ends (in place)
void trim(string& s)
{
	rtrim(s);
	ltrim(s);
}

typedef XLOPER* (WINAPI* p_XllFunction_1_t)(XLOPER* a1);
typedef XLOPER* (WINAPI* p_XllFunction_2_t)(XLOPER* a1, XLOPER* a2);
typedef XLOPER* (WINAPI* p_XllFunction_3_t)(XLOPER* a1, XLOPER* a2, XLOPER* a3);
typedef XLOPER* (WINAPI* p_XllFunction_4_t)(XLOPER* a1, XLOPER* a2, XLOPER* a3, XLOPER* a4);
typedef XLOPER* (WINAPI* p_XllFunction_5_t)(XLOPER* a1, XLOPER* a2, XLOPER* a3, XLOPER* a4, XLOPER* a5);
typedef XLOPER* (WINAPI* p_XllFunction_6_t)(XLOPER* a1, XLOPER* a2, XLOPER* a3, XLOPER* a4, XLOPER* a5, XLOPER* a6);
typedef XLOPER* (WINAPI* p_XllFunction_7_t)(XLOPER* a1, XLOPER* a2, XLOPER* a3, XLOPER* a4, XLOPER* a5, XLOPER* a6, XLOPER* a7);
typedef XLOPER* (WINAPI* p_XllFunction_8_t)(XLOPER* a1, XLOPER* a2, XLOPER* a3, XLOPER* a4, XLOPER* a5, XLOPER* a6, XLOPER* a7, XLOPER* a8);
typedef XLOPER* (WINAPI* p_XllFunction_9_t)(XLOPER* a1, XLOPER* a2, XLOPER* a3, XLOPER* a4, XLOPER* a5, XLOPER* a6, XLOPER* a7, XLOPER* a8, XLOPER* a9);
typedef XLOPER* (WINAPI* p_XllFunction_10_t)(XLOPER* a1, XLOPER* a2, XLOPER* a3, XLOPER* a4, XLOPER* a5, XLOPER* a6, XLOPER* a7, XLOPER* a8, XLOPER* a9, XLOPER* a10);

XLOPER* call_xll_function(p_XllFunction_t p, vector<XLOPER*> args)
{
	switch (args.size())
	{
	case 0:
		return (*p)();
	case 1:
		return (*(p_XllFunction_1_t)p)(args[0]);
	case 2:
		return (*(p_XllFunction_2_t)p)(args[0], args[1]);
	case 3:
		return (*(p_XllFunction_3_t)p)(args[0], args[1], args[2]);
	case 4:
		return (*(p_XllFunction_4_t)p)(args[0], args[1], args[2], args[3]);
	case 5:
		return (*(p_XllFunction_5_t)p)(args[0], args[1], args[2], args[3], args[4]);
	case 6:
		return (*(p_XllFunction_6_t)p)(args[0], args[1], args[2], args[3], args[4], args[5]);
	case 7:
		return (*(p_XllFunction_7_t)p)(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
	case 8:
		return (*(p_XllFunction_8_t)p)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
	case 9:
		return (*(p_XllFunction_9_t)p)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
	case 10:
		return (*(p_XllFunction_10_t)p)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
	default:
		fatal("Function call has too many arguments");
		return NULL;
	}
}

RowCol cellAddress(const char* addr)
{
	RowCol rc;

	const char* p = addr;
	char c = *p++;
	if (!(c >= 'A' && c <= 'Z'))
		fatal(xprintf("Invalid cell address: %s", addr));
	rc.col = c - 'A';

	try
	{
		int r = stoi(p);
		if (r < 1)
			fatal(xprintf("Invalid cell address: %s", addr));
		rc.row = r - 1;
	}
	catch (exception e1)
	{
		fatal(xprintf("Invalid cell address: %s", addr));
	}

	return rc;
}

XLOPER* cellRange(const char* addr)
{
	XLOPER* x = new XLOPER;
	x->xltype = xltypeSRef;
	x->val.sref.count = 1;

	// x64 Excel actually passes 0 rather than 1
	x->val.sref.count = 0;

	char* ap = _strdup(addr);
	char* p = strchr(ap, ':');
	if (p)
	{
		*p++ = 0;
		RowCol rc1 = cellAddress(ap);
		RowCol rc2 = cellAddress(p);

		x->val.sref.ref.colFirst = min(rc1.col, rc2.col);
		x->val.sref.ref.colLast = max(rc1.col, rc2.col);

		x->val.sref.ref.rwFirst = min(rc1.row, rc2.row);
		x->val.sref.ref.rwLast = max(rc1.row, rc2.row);
	}
	else
	{
		RowCol rc = cellAddress(ap);
		x->val.sref.ref.colFirst = x->val.sref.ref.colLast = rc.col;
		x->val.sref.ref.rwFirst = x->val.sref.ref.rwLast = rc.row;
	}
	
	free(ap);
	return x;
}