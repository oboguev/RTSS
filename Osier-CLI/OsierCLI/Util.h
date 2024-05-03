#pragma once

void fatal(const TCHAR* msg);
void fatal(const char* msg);
TCHAR* xprintf(const TCHAR* format, ...);
string GetLastErrorAsString(void);

string tchar2string(TCHAR* t);
char* to_counted_string(const char* s);
string from_counted_string(const char* s);
string op2string(const XLOPER* x);
vector<string> split(const string& str, const string& delim);

void ltrim(string& s);
void rtrim(string& s);
void trim(string& s);

typedef int (WINAPI* p_i_v_t)(void);
typedef XLOPER* (WINAPI* p_XllFunction_t)();
XLOPER* call_xll_function(p_XllFunction_t p, vector<XLOPER*> args);
XLOPER* cellRange(const char* addr);

class RowCol
{
public:
	int col;
	int row;
};
RowCol cellAddress(const char* addr);

static void noop()
{
}