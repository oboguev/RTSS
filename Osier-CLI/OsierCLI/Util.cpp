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