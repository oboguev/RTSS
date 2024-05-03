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
#include <unordered_map>
#include <iostream>
#include <regex>
#include <algorithm> 
#include <cctype>
#include <locale>

extern "C"
{
#include "..\XLCall32\e4v.h"
}

#include "Util.h"
#include "Value.h"
#include "XllFunctionArgument.h"
#include "XllFunction.h"
#include "Sheet.h"
#include "CLI.h"

extern HMODULE hOsierXLL;
extern unordered_map<string, XllFunction> xllFunctions;
extern Sheet sheet;

