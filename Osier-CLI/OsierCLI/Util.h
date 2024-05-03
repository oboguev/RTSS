#pragma once

void fatal(const TCHAR* msg);
void fatal(const char* msg);
TCHAR* xprintf(const TCHAR* format, ...);
std::string GetLastErrorAsString(void);

string tchar2string(TCHAR* t);
char* to_counted_string(const char* s);
string from_counted_string(const char* s);
string op2string(const XLOPER* x);
vector<string> split(const string& str, const string& delim);

void ltrim(std::string& s);
void rtrim(std::string& s);
void trim(std::string& s);

typedef int (WINAPI* p_i_v_t)(void);

static void noop()
{
}