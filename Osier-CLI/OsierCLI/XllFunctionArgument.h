#pragma once

class XllFunctionArgument
{
public:
	string name;
	
	// R = XLOPER(variant), A = boolean, B = double etc.
	// https://learn.microsoft.com/en-us/office/client-developer/excel/xlfregister-form-1
	// https://learn.microsoft.com/en-us/office/client-developer/excel/data-types-used-by-excel
	// https://smurfonspreadsheets.wordpress.com/2011/01/28/excel-xll-call-register-datatypes
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