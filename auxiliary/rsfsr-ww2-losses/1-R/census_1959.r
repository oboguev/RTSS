#
# Interpolate/graduate 1959 census data using toolset developed for UN World Population project
# 
#     https://timriffe.github.io/DemoTools/articles/graduation_with_demotools.html
#     https://timriffe.github.io/DemoTools/index.html
#     https://github.com/timriffe/DemoTools
#
# We use monotonic spline interpolation as the the smoothest and more natural, with least oscillations
#

library(DemoTools)

########################################

# example data set
#pop5_mat <- structure(
#    c(54170, 44775, 42142, 38464, 34406, 30386, 26933,
#      23481, 20602, 16489, 14248, 9928, 8490, 4801, 3599, 2048, 941,
#      326, 80, 17, 0, 57424, 44475, 41752, 39628, 34757, 30605, 27183,
#      23792, 20724, 17056, 14059, 10585, 8103, 5306, 3367, 2040, 963,
#      315, 80, 16, 1, 60272, 44780, 41804, 40229, 35155, 30978, 27456,
#      24097, 20873, 17546, 13990, 11146, 7841, 5738, 3184, 2062, 961,
#      311, 80, 15, 1, 62727, 45681, 42101, 40474, 35599, 31439, 27758,
#      24396, 21055, 17958, 14046, 11589, 7731, 6060, 3086, 2083, 949,
#      312, 79, 14, 1, 64816, 47137, 42508, 40532, 36083, 31940, 28092,
#      24693, 21274, 18299, 14223, 11906, 7785, 6255, 3090, 2084, 938,
#      316, 80, 14, 2),
#    .Dim = c(21L, 5L),
#    .Dimnames = list(seq(0, 100, by = 5), 1950:1954)
#)
#
#Value <- pop5_mat[,4]
#Age   <- as.integer(rownames(pop5_mat))

########################################

# interpolation points per year
ppy <- 100L

# census data with x-step = 1 point per year of age
#census_mat_basic <- structure(
#    c(2273515, 2057696, 2050408, 1202123, 917983, 824692, 1232709, 2092825, 2305946, 2518715, 2762422),
#   .Dim = c(11L,1L),
#   .Dimnames = list(10:20, 1959:1959)
#)  

# census data with x-step = ppy points per year of age
#census_mat_ppy <- structure(
#    c(2273515, 2057696, 2050408, 1202123, 917983, 824692, 1232709, 2092825, 2305946, 2518715, 2762422),
#   .Dim = c(11L,1L),
#   .Dimnames = list(seq(10 * ppy, 20 * ppy, by = ppy), 1959:1959)
#)  

# padded with extra data, to calm edge effects
#census_mat_ppy_padded <- structure(
#    c(2273515, 2057696, 2050408, 1202123, 917983, 824692, 1232709, 2092825, 2305946, 2518715, 2762422, round(2762422 * 1.1), round(2762422 * 1.2), round(2762422 * 1.3), 0),
#   .Dim = c(15L,1L),
#   .Dimnames = list(seq(10 * ppy, 24 * ppy, by = ppy), 1959:1959)
#)  

census_fuller_mat_ppy_padded <- structure(
    # actual 1959 data for ages 0-35
    c(2673692, 2650692, 2618103, 2676036, 2734270, 2504297, 2556631, 2475495, 2423412, 2455379, 
      2273515, 2057696, 2050408, 1202123,  917983,  824692, 1232709, 2092825, 2305946, 2518715, 
      2762422, 2624964, 2359786, 2063493, 1741259, 1750482, 2071898, 2020253, 2467978, 2280469, 
      2726990, 2250019, 2344838, 1957459, 1823606, 1651938,
      # artititial cutoff receding padding ages 36-47, total 48 elements
      1651938, 991163, 594698, 356819, 214091, 128455, 77073, 46244, 27746, 16648, 9989, 0),
   .Dim = c(48L,1L),
   .Dimnames = list(seq(0 * ppy, 47 * ppy, by = ppy), 1959:1959)
)  

#census_data <- census_mat_ppy_padded
census_data <- census_fuller_mat_ppy_padded

Value <- census_data[,1]
Age   <- as.integer(rownames(census_data))

########################################

validate <- function(title, data) {
    print(sprintf("Method: %s", title))

    #print(length(Value))
    #print(length(data))
    #print(ppy)

    print("Bin#  Age    AVG     RESULT      diff%  ")
    print("==== ==== ======== ========== ==========")
    nbuckets = length(Value) - 1
    for (nb in 1:nbuckets) {
        i1 = 1 + ppy*(nb-1)
        i2 = ppy*nb
        result <- mean(data[i1:i2]) * ppy
        pctdiff <- 100.0 * (result - Value[nb]) / Value[nb]
        print(sprintf("%4d %4d %8d %10.2f %10f", nb, Age[nb], Value[nb], result, pctdiff))
    }

    Sys.sleep(5)
}

dump_age_min <- 9
dump_age_max <- 21

dump_values <- function(data) {
    rn <- names(data)
    print("   Age              Interp          Avg   ")
    print("==========       ===========    ==========")
    for (i in 1:length(data)) {
        nb = 1 + ((i - 1) / ppy);
        age = as.double(rn[i]) / ppy
        if (age >= dump_age_min && age < dump_age_max) {
            print(sprintf("%10.3f    %14.3f    %10d", age, data[i] * ppy, Value[nb]))
        }
    }

    #print(data)
}

use_sprague <- FALSE
use_beers_ordinary <- FALSE
use_beers_modified <- FALSE
use_mono <- TRUE
use_pclm <- FALSE
use_unif <- FALSE

use_beers_any <- use_beers_ordinary || use_beers_modified

########################################

plot(Age, Value, type = 'b',
     ylab = 'Population',
     xlab = 'Age',
     main = 'Original 1-year age-group population')

Sys.sleep(5)
#validate("self", Value)

########################################

if (use_sprague)
{
    sprague <- graduate(Value, Age, method = "sprague")
    single.age  <- names2age(sprague)

    #sum(sprague)
    #sum(Value)

    plot(single.age, sprague, type = 'l',
         ylab = 'Population',
         xlab = 'Age',
         main = 'Graduation with Sprague method')

    validate("sprague", sprague)
}
    
########################################    
    
if (use_beers_any)
{
    beers.ordinary <- graduate(Value, Age, method = "beers(ord)")
    beers.modified <- graduate(Value, Age, method = "beers(mod)")
    single.age  <- names2age(beers.ordinary)

    plot(single.age, beers.ordinary, type = 'l',
         ylab = 'Population',
         xlab = 'Age',
         main = 'Graduation with Beers methods')
    lines(single.age,beers.modified, col = "red")
    legend("topright",lty=1,col=c(1,2),legend=c("ordinary (5-yr constrained)",
                                                "modified (smoother)"))
                                                                                   
    validate("beers.ordinary", beers.ordinary)
    validate("beers.modified", beers.modified)
}
                                                                                   
########################################    

if (use_mono)
{
    mono.grad <- graduate(Value, Age, method = "mono")
    single.age  <- names2age(mono.grad)

    plot(single.age, mono.grad, type = 'l',
         ylab = 'Population',
         xlab = 'Age',
         main = 'Graduation with monotone spline')
    
    validate("mono.grad", mono.grad)
    dump_values(mono.grad)
}

########################################    
    
if (use_pclm)
{
    pclm.grad <- graduate(Value, Age, method = "pclm")
    single.age  <- names2age(pclm.grad)

    plot(single.age, pclm.grad, type = 'l',
         ylab = 'Population',
         xlab = 'Age',
         main = 'Graduation with pclm')     
    
    validate("pclm.grad", pclm.grad)
}

########################################

if (use_unif)
{
    unif.grad <- graduate(Value, Age, method = "unif")
    single.age  <- names2age(unif.grad)

    plot(single.age, unif.grad, type = 'l',
         ylab = 'Population',
         xlab = 'Age',
     main = 'Graduation with uniform distribution')

    validate("unif.grad", unif.grad)
}

########################################

if (use_beers_any) {
    scale.line <- beers.modified
} else if (use_sprague) {
    scale.line <- sprague
} else if (use_mono) {
    scale.line <- mono.grad
} else if (use_pclm) {
    scale.line <- pclm.grad
} else if (use_unif) {
    scale.line <- unif.grad
}

plot.value = Value / ppy

plot(Age, plot.value, type = 'b',
     ylab = 'Population',
     xlab = 'Age',
     main = 'Graduation with multiple methods')

if (use_beers_ordinary)
{
    lines(single.age,beers.ordinary, col = "black", type = 'l')
}

if (use_beers_modified)
{
    lines(single.age,beers.modified, col = "red", type = 'l')
}

if (use_sprague)
{
    lines(single.age,sprague, col = "blue", type = 'l')
}

if (use_mono)
{
    lines(single.age,mono.grad, col = "green", type = 'l')
}

if (use_pclm)
{
    lines(single.age,pclm.grad, col = "pink", type = 'l')
}

if (use_unif)
{
    lines(single.age,unif.grad, col = "magenta", type = 'l')
}

lines(Age, Value, col="orange")

legend("topright", 
      lty=1,
      col=c('black','red','blue','green','pink','magenta'),
      legend=c("Beers ordinary", "Beers modified","Sprague","Monotone","PCLM", "Uniform"))

