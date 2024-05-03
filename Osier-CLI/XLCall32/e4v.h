#pragma once

typedef int (pascal *p_Excel4v_t)(int xlfn, LPXLOPER operRes, int count, LPXLOPER opers[]);
typedef void (_cdecl *p_setExcel4vInterceptor_t)(p_Excel4v_t p);
