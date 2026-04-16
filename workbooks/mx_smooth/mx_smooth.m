% ===== 1. Настройки =====
inputFile  = '/share/mx.xlsx';
inputSheet = 'INTERPOLATED';

outputFile = 'mx_smoothed.xlsx';
outputSheet = 'SMOOTHED';

% ===== 2. Чтение Excel с конкретного листа =====
T = readtable(inputFile, 'Sheet', inputSheet);

% Предполагается:
% 1-й столбец = год
% 2-й столбец = рождаемость
% 3-й столбец = смертность
year  = T{:,1};
birth = T{:,2};
death = T{:,3};

% ===== 3. Сглаживание =====
birth_smooth = smoothdata(birth, 'sgolay', 13);
death_smooth = smoothdata(death, 'sgolay', 9);

% ===== 4. Графики для контроля =====
figure;
plot(year, birth, '-o', 'DisplayName', 'Birth original');
hold on;
plot(year, birth_smooth, '-', 'LineWidth', 2, 'DisplayName', 'Birth smooth');
grid on;
xlabel('Year');
ylabel('Birth rate');
title('Birth rate: INTERPOLATED vs SMOOTHED');
legend('Location', 'best');

figure;
plot(year, death, '-o', 'DisplayName', 'Death original');
hold on;
plot(year, death_smooth, '-', 'LineWidth', 2, 'DisplayName', 'Death smooth');
grid on;
xlabel('Year');
ylabel('Death rate');
title('Death rate: INTERPOLATED vs SMOOTHED');
legend('Location', 'best');

% ===== 5. Подготовка вывода =====
Out = table(year, birth, birth_smooth, death, death_smooth, ...
    'VariableNames', {'Year','BirthInterpolated','BirthSmooth','DeathInterpolated','DeathSmooth'});

% ===== 6. Запись в новый Excel, на лист SMOOTHED =====
writetable(Out, outputFile, 'Sheet', outputSheet);

disp('Готово.');
disp(['Прочитан лист: ', inputSheet]);
disp(['Результат записан в файл: ', outputFile]);
disp(['Лист результата: ', outputSheet]);
