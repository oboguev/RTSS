#include "stdafx.h"

#define ONSTACK_LPXLOPER 32

static p_Excel4v_t p_Excel4v = NULL;

void xxx(p_Excel4v_t p)
{
}

__declspec(dllexport)
void _cdecl setExcel4vInterceptor(p_Excel4v_t p)
{
	p_Excel4v = p;
}

int _cdecl Excel4(int xlfn, LPXLOPER operRes, int count,... )
/* followed by count LPXLOPERs */
{
	va_list ap;
	int i;
	int result;

	LPXLOPER xls[ONSTACK_LPXLOPER];
	LPXLOPER* pxls = xls;
	if (count > ONSTACK_LPXLOPER)
		pxls = (LPXLOPER*) calloc(sizeof(LPXLOPER), count);

	va_start(ap, count);
	for (i = 0; i < count; i++)
		pxls[i] = va_arg(ap, LPXLOPER);
	va_end(ap);

	result = (*p_Excel4v)(xlfn, operRes, count, pxls);

	if (pxls != xls)
		free(pxls);

	return result;
}

int pascal Excel4v(int xlfn, LPXLOPER operRes, int count, LPXLOPER opers[])
{
	return (*p_Excel4v)(xlfn, operRes, count, opers);
}