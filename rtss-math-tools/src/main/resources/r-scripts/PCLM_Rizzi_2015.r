#
# PCLM code based on article
#    S. Rizzi et. al, "Efficient Estimation of Smooth Distributions From Coarsely Grouped Data" (2015)
#

pclm <- function(y, C, X, lambda = 1, deg = 2, show = F){

    # Fit a PCLM (estimate b in ) E(y) = C %*% exp(X %*% b)
    # y = the vector of observed counts of length i
    # C = the composition matrix of dimension IxJ
    # X = the identity matrix of dimension JxJ; or B-spline basis
    # lambda = smoothing parameter
    # deg = order of differences of the components of b
    # show = indicates whether iteration details should be shown

    # Fit the penalized composite link model

    # Some preparations
    nx <- dim(X)[2]
    D <- diff(diag(nx), diff=deg)
    la2 <- sqrt(lambda)
    it <- 0
    bstart <- log(sum(y) / nx);
    b <- rep(bstart, nx);

    # Perform the iterations
    for (it in 1:50) {
        b0 <- b
        eta <- X %*% b
        gam <- exp(eta)
        mu <- C %*% gam
        w <- c(1 / mu, rep(la2, nx - deg))
        Gam <- gam %*% rep(1, nx)
        Q <- C %*% (Gam * X)
        z <- c(y - mu + Q %*% b, rep(0, nx - deg))
        Fit <- lsfit(rbind(Q, D), z, wt = w, intercept = F)
        b <- Fit$coef
        db <- max(abs(b - b0))
        if (show) cat(it, " ", db, "\n")
        if (db < 1e-6) break
    }
    cat(it, " ", db, "\n")

    # Regression diagnostic
    R <- t(Q) %*% diag(c(1 / mu)) %*% Q
    H <- solve(R + lambda * t(D) %*% D) %*% R
    fit <- list()
    fit$trace <- sum(diag(H))
    ok <- y > 0 & mu > 0
    fit$dev <- 2 * sum(y[ok] * log(y[ok] / mu[ok]))
    fit$gamma <- gam
    fit$aic <- fit$dev + 2 * fit$trace
    fit$mu <- mu
    fit
}

# number of single-year points
m <-${m}

# number of bins
n <-${n}

# bin values
y <-${y}

# Make C matrix and (trivial) basis B
C <- matrix(0, n, m)
${fill_C}
B <- diag(m)

# smoothing parameter:
# no smoothing: lambda <- 0.0001
lambda <- ${lambda}

# solve PCLM
mod <- pclm(y, C, B,lambda = lambda, deg = 2)
# plot(mod$gamma)

cat("mod$gamma:", paste(seq(1:m), mod$gamma, sep = ":", collapse = ","), "\n")