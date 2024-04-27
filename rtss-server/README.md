### Using R packages from RTSS

RTSS uses R packages to disaggregate mortality curves and for other computations, and needs R installation it can invoke.

R can be installed either on the same machine as RTSS, or on a different machine. RTSS file `rtss-config.yml` points to the location of R. If R is installed on a remote machine, rtss-server must be ran on this machine too, and serves as a proxy between RTSS and R, relaying the calls.

R can also be installed within a virtual machine (such as VMWare or VirtualBox), running on the same (or different) physical host.

Installation sequence for R (assuming Ubuntu):

```
sudo apt update
sudo apt upgrade -y
sudo apt install -y gcc gdb g++ gfortran
sudo apt install -y curl wget gawk uuid dos2unix

sudo apt install -y libcurl4-openssl-dev libxml2-dev libssl-dev libfontconfig1-dev 
sudo apt install -y libharfbuzz-dev libfribidi-dev libfreetype6-dev libpng-dev libtiff5-dev libjpeg-dev
#
# on Fedora, CentOS, RHEL install: 
#    libcurl-devel libxml2-devel openssl-devel fontconfig-devel 
#    harfbuzz-devel fribidi-devel freetype-devel libpng-devel libtiff-devel libjpeg-devel
#
# on OSX (brew) install: 
#    openssl freetype harfbuzz fribidi (and maybe some more)
#

#
# follow instructions in https://phoenixnap.com/kb/install-r-ubuntu
#
sudo apt install -y software-properties-common dirmngr
wget -qO- https://cloud.r-project.org/bin/linux/ubuntu/marutter_pubkey.asc | sudo tee -a /etc/apt/trusted.gpg.d/cran_ubuntu_key.asc
gpg --show-keys /etc/apt/trusted.gpg.d/cran_ubuntu_key.asc
sudo add-apt-repository "deb https://cloud.r-project.org/bin/linux/ubuntu $(lsb_release -cs)-cran40/"
# press ENTER when promoted
sudo apt update
sudo apt install -y r-base r-base-dev

#
# verify that R was installed
#
$ R --vanilla --quiet
print("Hello, World!")
[Ctrl-D]

#
# optionally install RSudio, see https://posit.co/download/rstudio-desktop, e.g.
#
cd ~/Downloads
wget https://download1.rstudio.org/electron/jammy/amd64/rstudio-2023.12.1-402-amd64.deb
# may have to do
# sudo apt --fix-broken install
# sudo apt install -y libclang-dev
sudo dpkg -i rstudio*.deb
rstudio

#
# install DemoTools
#
MY_USER=`id -un`
MY_GROUP=`id -gn`
sudo chown $MY_USER:$MY_GROUP /usr/local/lib/R/site-library
R
# at R prompt enter:
install.packages("remotes")
install.packages("rstan", repos = c("https://mc-stan.org/r-packages/", getOption("repos")))
remotes::install_github("timriffe/DemoTools")
# if installation fails because additional Linux packages are needed, install those and retry R package installation

#
# install ungroup and MortalityLaws
#
install.packages("devtools")
devtools::install_github("mpascariu/ungroup")
devtools::install_github("mpascariu/MortalityLaws")
```
