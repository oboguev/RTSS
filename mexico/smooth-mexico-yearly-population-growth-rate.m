echo off
clear
tab = readtable('mexico-yearly-population-growth-rate.csv');
y = tab.YEAR;
r = tab.RATE;

plot(y,r,'DisplayName','RAW');
hold on 

%plot(y,smooth(y,r,0.1,'loess'),'DisplayName','LOESS 0.1');
%plot(y,smooth(y,r,0.1,'rloess'),'DisplayName','RLOESS 0.1');
%plot(y,smooth(y,r,0.1,'sgolay'),'DisplayName','SGOLAY 0.1');

%plot(y,smooth(y,r,0.05,'loess'),'DisplayName','LOESS 0.05');
plot(y,smooth(y,r,0.05,'rloess'),'DisplayName','RLOESS 0.05');
%plot(y,smooth(y,r,0.05,'sgolay'),'DisplayName','SGOLAY 0.05');

%[sp,values_02] = spaps(y,r,0.2);
%plot(y,values_02,'DisplayName','SPAPS 0.2');

%[sp,values_1_0] = spaps(y,r,1.0);
%plot(y,values_1_0,'DisplayName','SPAPS 1.0');

%[sp,values_5_0] = spaps(y,r,5.0);
%plot(y,values_5_0,'DisplayName','SPAPS 5.0');

[sp,values_9_0] = spaps(y,r,9.0);
plot(y,values_9_0,'DisplayName','SPAPS 9.0');

legend;
hold off

