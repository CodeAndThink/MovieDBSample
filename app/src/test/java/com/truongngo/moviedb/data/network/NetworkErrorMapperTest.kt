package com.truongngo.moviedb.data.network

import com.truongngo.moviedb.domain.model.MovieLoadError
import java.io.InterruptedIOException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class NetworkErrorMapperTest {
    @Test fun okhttpCallTimeoutIsNotAConnectionFailure() {
        assertEquals(MovieLoadError.TIMEOUT, NetworkErrorMapper.toMovieLoadError(InterruptedIOException("timeout")))
        assertEquals(MovieLoadError.CONNECTION, NetworkErrorMapper.toMovieLoadError(InterruptedIOException("interrupted")))
    }

    @Test fun cancellationIsRethrownUnchanged() {
        val cancellation = CancellationException("request cancelled")
        val thrown = assertThrows(CancellationException::class.java) {
            NetworkErrorMapper.toMovieLoadError(cancellation)
        }
        assertSame(cancellation, thrown)
    }
}
