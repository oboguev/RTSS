#pragma once

class XllFunction
{
public:
	// external name of the function
	string name;

	// name of exported DLL routine for the function
	string dllname;

	boolean is_void;
	char return_value_type;

	vector<XllFunctionArgument> args;

	string comment;

	XllFunction()
	{
	}

	XllFunction(const XllFunction& x)
	{
		name = x.name;
		dllname = x.dllname;
		is_void = x.is_void;
		args = x.args;
		comment = x.comment;
	}

	int numRequiredArgs()
	{
		for (int k = 0; k < args.size(); k++)
		{
			if (args[k].optional)
				return k;
		}

		return (int) args.size();
	}
};