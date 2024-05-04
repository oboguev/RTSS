#pragma once

enum ValueType { VT_None, VT_Integer, VT_Double, VT_String };

/*
 * Cell value
 */
class Value
{
protected:
	ValueType value_type;
	XLOPER m_xloper;
	char* ps_counted;

public:
	int i_value;
	double d_value;
	string s_value;

public:
	Value()
	{
		value_type = VT_None;
		ps_counted = NULL;
		d_value = 0;
		i_value = 0;
	}

	Value(const Value& x) : Value(&x)
	{
	}
	
	Value(const Value* x)
	{
		value_type = x->value_type;
		i_value = x->i_value;
		d_value = x->d_value;
		s_value = x->s_value;
		ps_counted = NULL;
	}

	Value(int v)
	{
		value_type = VT_Integer;
		i_value = v;
		d_value = 0;
		ps_counted = NULL;
	}

	Value(double v)
	{
		value_type = VT_Double;
		d_value = v;
		i_value = 0;
		ps_counted = NULL;
	}

	Value(const char* v)
	{
		value_type = VT_String;
		s_value = v;
		d_value = 0;
		i_value = 0;
		ps_counted = NULL;
	}

	Value(const XLOPER& x) : Value(&x)
	{
	}

	Value(const XLOPER* x)
	{
		switch (x->xltype & ~(xlbitXLFree | xlbitDLLFree))
		{
		case xltypeStr:
			value_type = VT_String;
			s_value = from_counted_string(x->val.str);
			break;

		case xltypeNum:
			value_type = VT_Double;
			d_value = x->val.num;
			break;

		case xltypeInt:
			value_type = VT_Integer;
			i_value = x->val.w;
			break;

		default:
			fatal(xprintf("Unexpected XLOPER type: 0x%04X", x->xltype));
		}

		ps_counted = NULL;
	}

	~Value()
	{
		if (ps_counted)
			free(ps_counted);
	}


	virtual ValueType type()
	{
		return value_type;
	}

	const char* toString()
	{
		char buf[50];

		switch (value_type)
		{
		case VT_Integer:
			sprintf(buf, "%d", i_value);
			s_value = buf;
			return s_value.c_str();

		case VT_Double:
			sprintf(buf, "%f", d_value);
			s_value = buf;
			return s_value.c_str();

		case VT_String:
			return s_value.c_str();

		case VT_None:
		default:
			return "";
		}
	}

	XLOPER* xloper(boolean asDouble)
	{
		switch (value_type)
		{
		case VT_Integer:
			if (asDouble)
			{
				m_xloper.xltype = xltypeNum;
				m_xloper.val.num = (double)i_value;
			}
			else
			{
				m_xloper.xltype = xltypeInt;
				m_xloper.val.w = i_value;
			}
			break;

		case VT_Double:
			m_xloper.xltype = xltypeNum;
			m_xloper.val.num = d_value;
			break;

		case VT_String:
			m_xloper.xltype = xltypeStr;
			if (ps_counted == NULL)
				ps_counted = to_counted_string(s_value.c_str());
			m_xloper.val.str = ps_counted;
			break;

		case VT_None:
			fatal("Cell with no value");
		}

		return &m_xloper;
	}
};
