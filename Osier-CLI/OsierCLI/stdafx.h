#pragma once

#define _CRT_SECURE_NO_WARNINGS 1

#include "targetver.h"

#include <windows.h>

#include <stdio.h>
#include <tchar.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <XLCALL.H>

using namespace std;
#include <string>
#include <vector>
#include <map>

extern "C"
{
#include "..\XLCall32\e4v.h"
}

#include "Util.h"
#include "Value.h"
#include "XllFunctionArgument.h"
#include "XllFunction.h"

extern HMODULE hOsierXLL;
extern map<string, XllFunction> xllFunctions;

