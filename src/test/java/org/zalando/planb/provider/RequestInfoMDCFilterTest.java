package org.zalando.planb.provider;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterConfig;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

public class RequestInfoMDCFilterTest {

    private RequestInfoMDCFilter filter;

    @Before
    public void setUp() throws Exception {
        filter = new RequestInfoMDCFilter();
    }

    @Test
    public void testInit() throws Exception {
        filter.init(new MockFilterConfig());
    }

    @Test
    public void testDestroy() throws Exception {
        filter.destroy();
    }

    @Test
    public void testDoFilterWithException() throws Exception {
        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final FilterChain mockFilterChain = mock(FilterChain.class);

        when(mockRequest.getMethod()).thenThrow(new RuntimeException());

        filter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verify(mockFilterChain).doFilter(same(mockRequest), same(mockResponse));
    }
}
