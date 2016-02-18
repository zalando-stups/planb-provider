package org.zalando.planb.provider;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.longThat;
import static org.mockito.Mockito.*;

public class MetricTest {

    private Metric metric;
    private MetricRegistry mockRegistry;
    private Timer mockTimer;

    @Before
    public void setUp() throws Exception {
        mockRegistry = mock(MetricRegistry.class);
        mockTimer = mock(Timer.class);

        metric = new Metric(mockRegistry);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(mockRegistry, mockTimer);
    }

    @Test
    public void testFinishButNotStarted() throws Exception {
        metric.finish("hello");
    }

    @Test
    public void testFailToCreateTimer() throws Exception {
        metric.start();

        Thread.sleep(50);

        when(mockRegistry.timer(anyString())).thenThrow(new IllegalStateException("oops"));

        metric.finish("hello");

        verify(mockRegistry).timer(eq("hello"));
    }

    @Test
    public void testFailToUpdateTimer() throws Exception {
        metric.start();

        Thread.sleep(50);

        when(mockRegistry.timer(anyString())).thenReturn(mockTimer);
        doThrow(new IllegalStateException("oops")).when(mockTimer).update(anyLong(), any());

        metric.finish("hello");

        verify(mockRegistry).timer(eq("hello"));
        verify(mockTimer).update(longThat(is(greaterThan(0L))), eq(MILLISECONDS));
    }

    @Test
    public void testSuccess() throws Exception {
        metric.start();

        Thread.sleep(50);

        when(mockRegistry.timer(anyString())).thenReturn(mockTimer);

        metric.finish("hello");

        verify(mockRegistry).timer(eq("hello"));
        verify(mockTimer).update(longThat(is(greaterThan(0L))), eq(MILLISECONDS));

    }
}
