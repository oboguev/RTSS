#include "stdafx.h"

void CLI::execute(const string& line)
{
	if (line.length() == 0)
		return;

	vector<string> tokens = split(line, " ");

	string verb = tokens[0];
	int argc = (int) tokens.size() - 1;

	if ((verb == "exit" || verb == "quit") && argc == 0)
	{
		cout << flush;
		fflush(stdout);
		fflush(stderr);
		exit(0);
	}
	else if (verb == "clear-sheet" && argc == 0)
	{
		sheet.clear();
	}
	else if (verb == "set-cell-empty" && argc == 1)
	{
		sheet.erase(tokens[1]);
	}
	else if (verb == "set-cell-string" && argc == 2)
	{
		sheet[tokens[1]] = Value(tokens[2].c_str());
	}
	else if (verb == "set-cell-integer" && argc == 2)
	{
		int value;

		try
		{
			value = stoi(tokens[2]);
		}
		catch (exception e1)
		{
			fprintf(stderr, "Invalid integer value: %s\n", tokens[2].c_str());
			return;
		}

		sheet[tokens[1]] = Value(value);
	}
	else if (verb == "set-cell-double" && argc == 2)
	{
		double value;

		try
		{
			value = stod(tokens[2], NULL);
		}
		catch (exception e1)
		{
			fprintf(stderr, "Invalid double value: %s\n", tokens[2].c_str());
			return;
		}

		sheet[tokens[1]] = Value(value);
	}
	else if (verb == "show-cells")
	{
		string args;
		for (int k = 1; k < tokens.size(); k++)
		{
			if (args.length() != 0)
				args += " ";
			args += tokens[k];
		}

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
						Value v = sheet.at(cell);
						xs += v.toString();
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
	else
	{
		fprintf(stderr, "Invalid command: %s\n", line.c_str());
	}
}
