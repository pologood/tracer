package org.zalando.tracer.servlet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zalando.tracer.Generator;
import org.zalando.tracer.Trace;
import org.zalando.tracer.Tracer;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class TracerFilterTest {

    private final String generatedValue = "c33a55a8-89e8-11e5-9ce2-dbc276b3cf7d";
    private final String presentValue = "c72f1216-89e8-11e5-af44-f3f1713f954e";

    private final Generator generator = mock(Generator.class);

    private final Tracer tracer = Tracer.builder()
            .trace("X-Trace-ID", generator)
            .build();

    private final Trace trace = tracer.get("X-Trace-ID");
    private final TracerFilter filter = new TracerFilter(tracer);

    @Rule
    public final JettyRule jetty = new JettyRule(filter, trace);

    @Before
    public void defaultBehaviour() {
        when(generator.generate()).thenReturn(generatedValue);
    }

    private String url(final String path) {
        return format("http://localhost:%d%s", jetty.getPort(), path);
    }

    @Test
    public void shouldAddPresentValueAsResponseHeader() throws Exception {
        given().
                when()
                .header("X-Trace-ID", presentValue)
                .get(url("/traced"))
                .then()
                .header("X-Trace-ID", presentValue);
    }

    @Test
    public void shouldAddGeneratedValueAsResponseHeader() {
        given().
                when()
                .get(url("/traced"))
                .then()
                .header("X-Trace-ID", generatedValue);
    }

    @Test
    public void shouldNotAddPresentValueAsResponseHeadersWhenTraceIsNotActive() throws Exception {
        given().
                when()
                .header("X-Trace-ID", presentValue)
                .get(url("/untraced"))
                .then()
                .header("X-Trace-ID", nullValue());
    }

    @Test
    public void shouldNotAddGeneratedValueAsResponseHeadersWhenTraceIsNotActive() throws Exception {
        given().
                when()
                .get(url("/untraced"))
                .then()
                .header("X-Trace-ID", nullValue());
    }

    @Test
    public void shouldManageTraceForAsyncDispatch() throws Exception {
        given().
                when()
                .get(url("/async"));

        verify(generator, times(1)).generate();
    }

    @Test
    public void shouldNotManageTraceForAsyncDispatch() throws Exception {
        given().
                when()
                .get(url("/async"));

        verify(generator, times(1)).generate();
    }

    @Test
    public void shouldNotManageTraceForForwardDispatch() throws Exception {
        given().
                when()
                .get(url("/forward"));

        verify(generator, times(1)).generate();
    }

    @Test
    public void shouldNotManageTraceForIncludeDispatch() throws Exception {
        given().
                when()
                .get(url("/include"));

        verify(generator, times(1)).generate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDenyNonHttpRequest() throws ServletException, IOException {
        filter.doFilter(mock(ServletRequest.class), mock(HttpServletResponse.class), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDenyNonHttpResponse() throws ServletException, IOException {
        filter.doFilter(mock(HttpServletRequest.class), mock(ServletResponse.class), null);
    }

}