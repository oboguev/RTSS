#pragma once

class XllFunctionArgument
{
public:
	string name;
	
	// R = XLOPER(variant), A = boolean, B = double etc.
	char type;
	
	string comment;
	
	boolean optional;

	XllFunctionArgument()
	{
	}

	XllFunctionArgument(const XllFunctionArgument& x)
	{
		name = x.name;
		type = x.type;
		comment = x.comment;
		optional = x.optional;
	}
};