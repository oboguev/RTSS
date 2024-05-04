#include "stdafx.h"

static HMODULE hXLCall32 = NULL;
HMODULE hOsierXLL = NULL;
unordered_map<string, XllFunction> xllFunctions;
Sheet sheet;

void load_libraries();
int pascal MyExcel4v(int xlfn, LPXLOPER r, int count, LPXLOPER op[]);
void coerce(XLOPER* r, XLREF ref, int w);
void coerce_multi(XLOPER* r, XLREF ref);

/* groundwork for pooled allocation (tbd later) */
static void* palloc(size_t size) { return malloc(size); }
static void* pcalloc(size_t num, size_t size) { return calloc(num, size);  }

int _tmain(int argc, _TCHAR* argv[])
{
	load_libraries();

	CLI cli;

	for (string line; getline(cin, line); )
	{
		line = regex_replace(line, regex("\t"), " ");
		line = regex_replace(line, regex(" +"), " ");
		trim(line);
		cli.execute(line);
	}

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
		// op[2] is argument types such as "RRRRR"; first entry is function return type
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
		f.name = op2string(op[3]);
		f.comment = op2string(op[9]);

		s = op2string(op[5]);
		if (s == "1")
			f.is_void = false;
		else if (s == "2")
			f.is_void = true;
		else
			fatal("Unexpected function definition (isvoid)");
		
		string s_argtypes = op2string(op[2]);
		const char* argtypes = s_argtypes.c_str();
		vector<string> argnames = split(op2string(op[4]), ",");

		if (f.name == "MakeVector" && argnames.size() == 20)
			argtypes = "RRRRRRRRRRRRRRRRRRRRR";
		else if (f.name == "MakeMatrix" && argnames.size() == 20)
			argtypes = "RRRRRRRRRRRRRRRRRRRRR";
		else if (f.name == "GetObj" && argnames.size() == 2)
			argtypes = "RRR";
		else if (f.name == "ModifyObj" && argnames.size() == 19)
			argtypes = "RRRRRRRRRRRRRRRRRRRR";

		if (strlen(argtypes) != 1 + argnames.size())
			fatal("Unexpected function definition (arg count mismatch)");

		f.return_value_type = argtypes[0];
		for(int k = 0; k < argnames.size(); k++)
		{
			XllFunctionArgument arg;
			arg.type = argtypes[k + 1];
			string name = argnames[k];
			if (name[0] == '[' && name.back() == ']')
			{
				arg.optional = true;
				name = name.substr(1, name.length() - 2);
			}
			else
			{
				arg.optional = false;
			}
			arg.name = name;
			
			int cix = 10 + k;
			if (cix >= count)
				fatal("Unexpected function definition (missing argument description)");
			arg.comment = op2string(op[cix]);

			f.args.push_back(arg);
		}

		xllFunctions[f.name] = f;

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
	else if (xlfn == xlCoerce && count == 2 && 
			op[0]->xltype == xltypeSRef && op[0]->val.sref.count == 1 &&
			op[1]->xltype == xltypeInt)
	{
		coerce(r, op[0]->val.sref.ref, op[1]->val.w);
    }
	else
	{
		fatal(xprintf("Unknown Excel function %d\n", xlfn));
	}

	return xlretSuccess;
}

/* ========================================================================================= */

static void coerce(XLOPER* r, XLREF ref, int w)
{
	if (w == xltypeMulti)
		coerce_multi(r, ref);
	else
		fatal("Plugin requested xlCoerce to unsupported type");
}

static void coerce_multi(XLOPER* r, XLREF ref)
{
	int ncol = ref.colLast - ref.colFirst + 1;
	int nrow = ref.rwLast - ref.rwFirst + 1;

	r->xltype = xltypeMulti;
	r->val.array.rows = nrow;
	r->val.array.columns = ncol;
	r->val.array.lparray = (XLOPER*)pcalloc(nrow * ncol, sizeof XLOPER);

	for (int col = ref.colFirst; col <= ref.colLast; col++)
	{
		for (int row = ref.rwFirst; row <= ref.rwLast; row++)
		{
			char cellname[30];
			sprintf(cellname, "%c%d", 'A' + col, row + 1);

			Value v;

			try
			{
				v = sheet.at(cellname);
			}
			catch (out_of_range ex)
			{
				noop();
			}

			noop();
			// ###
		}
	}
}
