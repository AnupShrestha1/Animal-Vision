package com.animalvision.core.color

/** Minimal linear-algebra helpers (small matrices only). */
object Mat {
    fun mul(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val rows = a.size; val inner = b.size; val cols = b[0].size
        val out = Array(rows) { DoubleArray(cols) }
        for (i in 0 until rows) for (j in 0 until cols) {
            var s = 0.0
            for (k in 0 until inner) s += a[i][k] * b[k][j]
            out[i][j] = s
        }
        return out
    }

    fun mulv(a: Array<DoubleArray>, v: DoubleArray): DoubleArray {
        val out = DoubleArray(a.size)
        for (i in a.indices) {
            var s = 0.0
            for (k in v.indices) s += a[i][k] * v[k]
            out[i] = s
        }
        return out
    }

    /** Row vector (1×N) · matrix (N×M) -> row vector (1×M). */
    fun mulRow(v: DoubleArray, a: Array<DoubleArray>): DoubleArray {
        val cols = a[0].size
        val out = DoubleArray(cols)
        for (j in 0 until cols) {
            var s = 0.0
            for (k in v.indices) s += v[k] * a[k][j]
            out[j] = s
        }
        return out
    }

    fun transpose(a: Array<DoubleArray>): Array<DoubleArray> {
        val out = Array(a[0].size) { DoubleArray(a.size) }
        for (i in a.indices) for (j in a[i].indices) out[j][i] = a[i][j]
        return out
    }

    /** Determinant of a 3x3 matrix. */
    fun det3(m: Array<DoubleArray>): Double =
        m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) -
            m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) +
            m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0])

    /** Inverse of a 3x3 matrix. */
    fun inv3(m: Array<DoubleArray>): Array<DoubleArray> {
        val d = det3(m)
        require(Math.abs(d) > 1e-12) { "singular 3x3 matrix" }
        return arrayOf(
            doubleArrayOf(
                (m[1][1] * m[2][2] - m[1][2] * m[2][1]) / d,
                (m[0][2] * m[2][1] - m[0][1] * m[2][2]) / d,
                (m[0][1] * m[1][2] - m[0][2] * m[1][1]) / d,
            ),
            doubleArrayOf(
                (m[1][2] * m[2][0] - m[1][0] * m[2][2]) / d,
                (m[0][0] * m[2][2] - m[0][2] * m[2][0]) / d,
                (m[0][2] * m[1][0] - m[0][0] * m[1][2]) / d,
            ),
            doubleArrayOf(
                (m[1][0] * m[2][1] - m[1][1] * m[2][0]) / d,
                (m[0][1] * m[2][0] - m[0][0] * m[2][1]) / d,
                (m[0][0] * m[1][1] - m[0][1] * m[1][0]) / d,
            ),
        )
    }

    /**
     * Moore–Penrose pseudo-inverse of a 3×N matrix with full row rank, via
     * pinv(A) = A^T (A A^T)^{-1}.
     */
    fun pinv3xN(a: Array<DoubleArray>): Array<DoubleArray> {
        require(a.isNotEmpty() && a[0].size > 0)
        val rows = a.size
        require(rows == 3) { "pinv3xN expects 3 rows" }
        val aT = transpose(a)
        val aaT = mul(a, aT) // 3x3
        val inv = inv3(aaT)
        return mul(aT, inv)  // N x 3
    }
}
