package com.xwal.adapter.web.rest.filter

import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

@Filter("/api/**")
class ApiVersionResponseFilter : HttpServerFilter {

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        return Flux.from(chain.proceed(request)).map { response ->
            response.header("X-XWAL-Version", API_VERSION)
            response
        }
    }

    companion object {
        const val API_VERSION = "2.0.0"
    }
}
