program main_program

call census_1920_bins_to_yearly
!call weather_temperature_monthly_to_daily

end program main_program

!***************************************************
! Original example of use
!***************************************************
subroutine weather_temperature_monthly_to_daily

use parametersmod
use newsplinemod,   only : newspline

implicit none

character(6) :: header
real(sp),    dimension(53) :: temp_F, temp_C
integer(i4), dimension(53) :: date

real(sp),    dimension(36) :: temp
integer(i4), dimension(36) :: nk
real(sp),    dimension(2)  :: bcond
real(sp),    dimension(:), allocatable :: daydata

integer(i4)  :: i
integer(i4)  :: j
integer(i4)  :: n
integer(i4)  :: summ

write(*,*) "Interpolated weather (temperature) data"

!------
! Read temperature data file
open(10, file = "captaincook_monthly_temp_NOAA.txt", status = 'old')

read(10,*) header

do i = 1, 53

  read(10,*) date(i), temp_F(i), temp_C(i)

end do

!---
! Define number of days in each month
nk = [31,28,31,30,31,30,31,31,30,31,30,31,  &
      31,29,31,30,31,30,31,31,30,31,30,31,  &
      31,28,31,30,31,30,31,31,30,31,30,31]

!---
! Initiate temperature array. Copy first and last month to start and end.
summ = sum(nk)

allocate(daydata(summ))

daydata = 0.0

temp(1:36) = temp_C(1:36)

! Apply circular boundary conditions by copy last interval to the beginning and vice versa
bcond(1) = temp_C(36)
bcond(2) = temp_C(1)

!---
! Interpolation without any limit options
call newspline(temp,nk,bcond,daydata)

!---
! Print newspline output
n = 1

do i = 1, 36

  do j = 1, nk(i)

    write(*,*) n, temp(i), daydata(n)

    n = n + 1

  end do

end do

end subroutine weather_temperature_monthly_to_daily

!*******************************************************************
! 1920 census data: interpolate aggregated bins into yearly age data
!*******************************************************************
subroutine census_1920_bins_to_yearly

use parametersmod
use newsplinemod,   only : newspline

implicit none

integer(i4)  :: nbins, nb, nyears, year, ix, yx, yx2, k, points_per_year, npoints
integer(i4), dimension(:), allocatable :: age_x1, age_x2, bin_widths_years, bin_widths_points
real(sp),    dimension(:), allocatable :: bin_values
real(sp),    dimension(:), allocatable :: result_values, result_averages
real(sp),    dimension(2)  :: bcond
real(4)      :: age, pct
real(sp),    dimension(:), allocatable :: year_values, result_bin_values
real(sp)     :: llim
character(1) :: tab
character(2) :: tab2
character(3) :: tab3

! number of bins in source data
nbins = 10

! how many result points we want to calculate per every single year
! i.e. x-resolution of the produced curve
points_per_year = 100

allocate(age_x1(nbins))
allocate(age_x2(nbins))
allocate(bin_values(nbins))
allocate(bin_widths_years(nbins))
allocate(bin_widths_points(nbins))

!------
! Read data file
open(10, file = "census_1920_source.txt", status = 'old')

do nb = 1, nbins

  read(10,*) age_x1(nb), age_x2(nb), bin_values(nb)
  bin_widths_years(nb) = age_x2(nb) - age_x1(nb)

end do

nyears = sum(bin_widths_years)
npoints = nyears * points_per_year
allocate(result_values(npoints))
allocate(result_averages(npoints))

bin_widths_points = bin_widths_years * points_per_year 

ix = 1
do nb = 1, nbins
    do k = 1, bin_widths_years(nb) * points_per_year 
        result_averages(ix) = bin_values(nb)
        ix = ix + 1
    end do
end do

!bcond(1) = bin_values(1)
bcond(1) = 0
bcond(2) = 0
llim = 0

call newspline(monthdata=bin_values, nk=bin_widths_points, bcond=bcond, daydata=result_values, llim=llim)

! print the results of interpolation
! for each point: age, interpoladed value, average value of the corresponding bin
write(*,*) "Interpolated population data, by years of age, continuous curve:"
write(*,*) ""
do ix = 1, npoints
    age = age_x1(1) + (ix - 1) * real(age_x2(nbins) - age_x1(1), 4) / npoints
    !!!write(*,*) age, result_values(ix), result_averages(ix)
end do

!-----------------------------------------------------------
! verify whether per-bin average is preserved by the spline
!-----------------------------------------------------------

! reaggregate by year
allocate(year_values(nyears))
year_values(nyears) = 0.0

do ix = 1, npoints
    yx = 1 + (ix - 1) / points_per_year
    year_values(yx) = year_values(yx) + result_values(ix)
end do

year_values = year_values / points_per_year

write(*,*) ""
write(*,*) "****************************************************************"
write(*,*) "Interpolated population data, by years of age, for a whole year:"
write(*,*) ""
do yx = 1, nyears
    write(*,*) age_x1(1) + (yx - 1), year_values(yx)
end do

! reaggregate by bin
allocate(result_bin_values(nbins))
result_bin_values = 0

yx = 1
do nb = 1, nbins
    yx2 = yx + bin_widths_years(nb)
    result_bin_values(nb) = sum(year_values(yx:yx2-1)) / bin_widths_years(nb)
    yx = yx2
end do

write(*,*) ""
write(*,*) "****************************************************************"
write(*,*) "Reaggregated population data, by bins:"
write(*,*) ""

tab = achar(9)
tab2 = tab // tab
tab3 = tab2 // tab

write(*,*) "    year    year       original    reaggregated        diff %"
do nb = 1, nbins
    pct = 100 * (result_bin_values(nb) - bin_values(nb)) / bin_values(nb) 
    write(*,"(I8,I8,F16.4,F16.4,F16.8)") age_x1(nb), age_x2(nb), bin_values(nb), result_bin_values(nb), pct
end do

end subroutine census_1920_bins_to_yearly