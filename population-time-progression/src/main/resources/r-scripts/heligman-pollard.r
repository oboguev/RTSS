#
# Determine coefficients for Heligman-Pollard mortality law curve
# fitting given age-specific mortality rates  
#

library(MortalityLaws)

#
# define age bins, as start of age intervals
# last bin is open-ended
#
x <- ${bin_start_age}

#
# vector with per-bin death counts in a year
#
y <- ${death_count}

#
# exposure vector, i.e. per-bin number of people at the start of the year 
#
exposure <- ${exposure}

#x <- c(0, 1, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85)
#y <- c(146.2, 4 * 21.6, 5 * 4.2, 5 * 2.9, 5 * 3.5, 5 * 5.2, 5 * 6.0, 5 * 7.6, 5 * 9.5, 5 * 13.6, 5 * 18.2, 5 * 22.9, 5 * 30.7, 5 * 40.8, 5 * 56.2, 5 * 78.9, 5 * 109.9, 5 * 165.65, 16 * 221.41)
#exposure <- 1000 * c(1, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 16)

#
# use LF2, can also sometimes use LF5
#
fit <- MortalityLaw(x   = x,
                    Dx  = y,   # vector with death counts
                    Ex  = exposure, # vector containing exposures
                    law = "HP",
                    opt.method = "LF2")
#plot(fit)

cat("fit$opt.diagnosis$message:", fit$opt.diagnosis$message, "\n")
cat("fit$deviance:", fit$deviance, "\n")
cat("fit$coefficients:", paste(names(fit$coefficients), fit$coefficients, sep = ":", collapse = ","), "\n")
cat("fit$residuals:", paste(names(fit$residuals), fit$residuals, sep = ":", collapse = ","), "\n")

#Example output:
#fit$opt.diagnosis$message: function evaluation limit reached without convergence (9) 
#fit$deviance: 5.820798e-05 
#fit$coefficients: A:0.023015645452356,B:0.0525300825459925,C:0.257545159036299,D:2.62975794141521e+22,E:0.00636316025439506,F_:3.1925017823177e+42,G:0.000160365995070258,H:1.09104599078948 
#fit$residuals: 0:3.24779361199068e-05,1:-7.57230761681375e-05,5:5.22042572978099e-05,10:-6.83018193803524e-05,15:-7.0408277474079e-05,20:0.00050751425841902,25:-0.00015288030736083,30:-0.000368827010466452,35:-0.000751692166800544,40:0.000395762117645184,45:0.00104983927290407,50:0.000313203789048297,55:0.000437412289453255,60:-0.000481028605958639,65:-0.00102428936433223,70:-0.0013662418493955,75:-0.00329629394331281,80:0.006449435367123,85:0.000225088956863434

#can also say:
# relative convergence (4)
# false convergence (8)
# want deviance << 1e-4  
 