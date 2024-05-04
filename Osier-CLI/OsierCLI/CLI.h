#pragma once

class CLI
{
public:
	void execute(const string& line);

private:
	void do_call(const string& retval, const string& fname, vector<string>& args);
	string concat(const vector<string> tokens, const string& sep);
	void show_sheet(void);
};
