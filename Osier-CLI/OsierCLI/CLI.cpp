#include "stdafx.h"

/*
 * set-cell-empty A1
 * set-cell-string A1 text
 * set-cell-integer A1 123
 * set-cell-double A1 14.32
 * echo aaa bbb ccc
 * show cells "A1: " A1 ", A2: " A2 ", A3: " A3
 * call Z1 func A1 B1:B3 C1:E3
 * clear-sheet
 * exit
 * quit
 */

void CLI::execute(const string& line)
{
	if (line.length() == 0 || line[0] == '#')
		return;

	vector<string> tokens = split(line, " ");

	string verb = tokens[0];
	tokens.erase(tokens.begin());
	int argc = (int) tokens.size();

	if ((verb == "exit" || verb == "quit") && argc == 0)
	{
		cout << flush;
		fflush(stdout);
		fflush(stderr);
		exit(0);
	}
	else if (verb == "clear-sheet" && argc == 0)
	{
		vector<Value*> vals;
		vals.reserve(sheet.size());
		for (auto kv : sheet)
			vals.push_back(kv.second);
		sheet.clear();
		for (Value* v : vals)
			delete v;
	}
	else if (verb == "set-cell-empty" && argc == 1)
	{
		sheet.delete_cell(tokens[0]);
	}
	else if (verb == "set-cell-string" && argc >= 2)
	{
		string cell = tokens[0];
		tokens.erase(tokens.begin());
		string args = concat(tokens, " ");
		sheet.delete_cell(cell);
		sheet[cell] = new Value(args.c_str());
	}
	else if (verb == "set-cell-integer" && argc == 2)
	{
		int value;

		try
		{
			value = stoi(tokens[1]);
		}
		catch (exception e1)
		{
			fprintf(stderr, "Invalid integer value: %s\n", tokens[1].c_str());
			return;
		}

		sheet.delete_cell(tokens[0]);
		sheet[tokens[0]] = new Value(value);
	}
	else if (verb == "set-cell-double" && argc == 2)
	{
		double value;

		try
		{
			value = stod(tokens[1], NULL);
		}
		catch (exception e1)
		{
			fprintf(stderr, "Invalid double value: %s\n", tokens[1].c_str());
			return;
		}

		sheet.delete_cell(tokens[0]);
		sheet[tokens[0]] = new Value(value);
	}
	else if (verb == "echo")
	{
		string args = concat(tokens, " ");
		cout << args << endl;
	}
	else if (verb == "show-cells")
	{
		string args = concat(tokens, " ");
		string xs;
		boolean inquote = false;
		string cell;
		for (const char* cp = args.c_str(); ; )
		{
			char c = *cp++;

			if (c == '"')
			{
				inquote = !inquote;
			}
			else if (c == 0 && inquote)
			{
				inquote = false;
			}
			else if (inquote)
			{
				xs += c;
			}
			else if (c == ' ' || c == 0)
			{
				if (cell.length() != 0)
				{
					try
					{
						Value* v = sheet.at(cell);
						xs += v->toString();
					}
					catch (out_of_range ex)
					{
						xs += "#NOVALUE";
					}
				}
				cell = "";
			}
			else
			{
				cell += c;
			}

			if (c == 0)
				break;
		}

		cout << xs << endl;
	}
	else if (verb == "call" && argc >= 0)
	{
		/*
		* call return-value-cell function-name args...
		*/
		string retval = tokens[0];
		tokens.erase(tokens.begin());

		string fname = tokens[0];
		tokens.erase(tokens.begin());

		do_call(retval, fname, tokens);
	}
	else if (verb == "show-sheet" && argc == 0)
	{
		show_sheet();
    }
	else
	{
		fprintf(stderr, "Invalid command: %s\n", line.c_str());
	}
}

void CLI::do_call(const string& retval, const string& fname, vector<string>& args)
{
	if (xllFunctions.find(fname) == xllFunctions.end())
	{
		cerr << "Function " << fname << " is not defiend" << endl << flush;
		return;
	}

	XllFunction f = xllFunctions[fname];
	if (args.size() < f.numRequiredArgs())
	{
		cerr << "Insufficient number of argument for function " << fname << endl << flush;
		return;
	}

	if (args.size() > f.args.size())
	{
		cerr << "Too many arguments for function " << fname << endl << flush;
		return;
	}

	p_XllFunction_t pfunc = (p_XllFunction_t) GetProcAddress(hOsierXLL, f.dllname.c_str());
	if (pfunc == NULL)
		fatal(xprintf("Unable to access function %s)", f.dllname.c_str()));

	vector<XLOPER*> vop;

	for (int k = 0; k < args.size(); k++)
	{
		XLOPER* x = cellRange(args[k].c_str());
		vop.push_back(x);
	}

	XLOPER* xres = call_xll_function(pfunc, vop);

	for (int k = 0; k < vop.size(); k++)
		delete vop[k];

	sheet.delete_cell(retval);
	Value* rv = new Value(xres);
	sheet[retval] = rv;

	if (rv->type() == VT_String)
	{
		string s = rv->toString();
		char* xp = _strdup(s.c_str());
		xp = _strlwr(xp);
		if (strstr(xp, "err") == xp)
			fatal(xprintf("Function %s returned error: %s", fname.c_str(), s.c_str()));
		free(xp);
	}
}

string CLI::concat(const vector<string> tokens, const string& sep)
{
	string res;
	for (int k = 0; k < tokens.size(); k++)
	{
		if (res.length() != 0)
			res += sep;
		res += tokens[k];
	}

	return res;
}

void CLI::show_sheet(void)
{
	char max_col = 'A';
	int max_row = 1;

	for (auto kv : sheet)
	{
		string cell = kv.first;
		char col = cell[0];
		int row = atoi(cell.c_str() + 1);

		if (col > max_col)
			max_col = col;
		if (row > max_row)
			max_row = row;
	}

	for (int row = 1; row <= max_row; row++)
	{
		for (char col = 'A'; col <= max_col; col++)
		{
			if (col != 'A')
				printf(",");
			char cell[30];
			sprintf(cell, "%c%d", col, row);

			Value* v = NULL;

			try
			{
				v = sheet.at(cell);
			}
			catch (out_of_range ex)
			{
				noop();
			}

			if (v != NULL)
			{
				if (v->type() == VT_String)
					printf("\"");
				printf("%s", v->toString());
				if (v->type() == VT_String)
					printf("\"");
			}
		}

		printf("\n");
	}
}
