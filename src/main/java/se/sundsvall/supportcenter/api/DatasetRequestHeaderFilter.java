package se.sundsvall.supportcenter.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpMethod.POST;

/**
 * Returns the adv-dataset-request header of a request to register an end of lease batch unchanged on the response. It
 * is set before the request is handled, the same way dept44 sets x-request-id, so that every response carries it
 * whatever the status.
 */
@Component
class DatasetRequestHeaderFilter extends OncePerRequestFilter {

	static final String DATASET_REQUEST_HEADER = "adv-dataset-request";

	private static final PathPattern END_OF_LEASE_BATCHES = PathPatternParser.defaultInstance.parse("/{municipalityId}/endOfLeaseBatches");

	@Override
	protected boolean shouldNotFilter(final HttpServletRequest request) {
		return !POST.matches(request.getMethod())
			|| !END_OF_LEASE_BATCHES.matches(RequestPath.parse(request.getRequestURI(), request.getContextPath()).pathWithinApplication());
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException {
		ofNullable(request.getHeader(DATASET_REQUEST_HEADER)).filter(StringUtils::hasText).ifPresent(value -> response.setHeader(DATASET_REQUEST_HEADER, value));
		chain.doFilter(request, response);
	}
}
