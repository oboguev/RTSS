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

# interpolation points per year
ppy <- 100L

rsfsr_1959_census_mat_ppy_padded <- structure(
    # actual 1959 census RSFSR data for ages 0-35
    c(2673692, 2650692, 2618103, 2676036, 2734270, 2504297, 2556631, 2475495, 2423412, 2455379, 
      2273515, 2057696, 2050408, 1202123,  917983,  824692, 1232709, 2092825, 2305946, 2518715, 
      2762422, 2624964, 2359786, 2063493, 1741259, 1750482, 2071898, 2020253, 2467978, 2280469, 
      2726990, 2250019, 2344838, 1957459, 1823606, 1651938,
      # artititial cutoff with receding padding, ages 36-47, total 48 elements
      1651938, 991163, 594698, 356819, 214091, 128455, 77073, 46244, 27746, 16648, 9989, 0),
   .Dim = c(48L,1L),
   .Dimnames = list(seq(0 * ppy, 47 * ppy, by = ppy), 1959:1959)
)  

ussr_1959_census_mat_ppy_padded <- structure(
    # actual 1959 census USSR data for ages 0-35
    c(4941943, 4814717, 4824744, 4860641, 4891170, 4494379, 4578178, 4373766, 4365670, 4217154,
      4049353, 3536842, 3576594, 2216973, 1957440, 1796204, 2503106, 3660502, 4194923, 4316538,
      4839038, 4548304, 4193488, 3653926, 3108272, 3101726, 3572820, 3417968, 4279726, 3817889,
      4744064, 3769136, 3962259, 3353473, 3169967, 2962885, 
      # artititial cutoff with receding padding, ages 36-47, total 48 elements
      2962885, 1777731, 1066639, 639983, 383990, 230394, 138236, 82942, 49765, 29859, 17915, 0),
   .Dim = c(48L,1L),
   .Dimnames = list(seq(0 * ppy, 47 * ppy, by = ppy), 1959:1959)
)  

census_data <- rsfsr_1959_census_mat_ppy_padded
#census_data <- ussr_1959_census_mat_ppy_padded

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

