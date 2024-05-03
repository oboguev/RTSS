#pragma once

void fatal(const TCHAR* msg);
void fatal(const char* msg);
TCHAR* xprintf(const TCHAR* format, ...);
std::string GetLastErrorAsString(void);

string tchar2string(TCHAR* t);
char* to_counted_string(const char* s);
string from_counted_string(const char* s);

typedef int (WINAPI* p_i_v_t)(void);

static void noop()
{
}