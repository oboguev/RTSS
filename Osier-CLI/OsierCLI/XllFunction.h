#pragma once

class XllFunction
{
public:
	// external name of the function
	string name;

	// name of exported DLL routine for the function
	string dllname;

	boolean is_void;

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
};