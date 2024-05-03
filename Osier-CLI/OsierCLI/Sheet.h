#pragma once

class Sheet : public unordered_map<string,Value>
{
public:
	string cell(int col, int row)
	{
		char buf[50];
		sprintf(buf, "%c%d", 'A' + col, 1 + row);
		string s = buf;
		return s;
	}
};