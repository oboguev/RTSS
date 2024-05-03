#pragma once

class CLI
{
public:
	void execute(const string& line);

private:
	void do_call(const string& retval, const string& fname, vector<string>& args);
};
