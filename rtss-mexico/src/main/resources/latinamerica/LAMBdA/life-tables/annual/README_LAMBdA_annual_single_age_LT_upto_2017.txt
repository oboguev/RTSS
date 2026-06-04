PIVOTAL ABRIDGED LIFE TABLES
Adjusted PIVOTAL abridged life tables (five-year age groups) regardless of method used to generate them.

The file includes the following: 

  obs:         188,684                          
 vars:            17                          
-----------------------------------
              storage   display   
variable name   type    format    
-----------------------------------
ctry            float   %18.0g     
year            float   %9.0g                 
age             float   %9.0g                 
Mx_m            float   %8.6f                 
Mx_f            float   %9.0g                 
Qx_m            float   %8.6f                 
Qx_f            float   %8.6f                 
lx_m            double  %9.0f                 
lx_f            double  %9.0f                 
Lx_m            float   %9.0f                 
Lx_f            float   %9.0f                 
Tx_m            double  %9.0f                 
Tx_f            double  %9.0f                 
Ex_m            float   %6.3f                 
Ex_f            float   %6.3f                 
piv             float   %9.0g  
method          float   %14.0g 
-----------------------------------

There are 5 methods used to generate the life tables (see column "method"):
1. OGIVE, 
2. Bennett-Horiuchi (BH), 
3. Blending (BLEND) methods 1 & 2, 
4. Interpolating PIVOTAL life tables constructed with OGIVE (OGIVE interpol)
5. Using data from Vital Statistics (VITAL stats)

See the documentation for a full description of these methods and their application in LAMBdA.
