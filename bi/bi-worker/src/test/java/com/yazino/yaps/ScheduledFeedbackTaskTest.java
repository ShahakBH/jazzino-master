package com.yazino.yaps;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ScheduledFeedbackTaskTest {

    private final Map<String, FeedbackService> services = new HashMap<String, FeedbackService>();
    private final ScheduledFeedbackTask task = new ScheduledFeedbackTask(services);

    @Test
    public void shouldAttemptToReadFeedbackForEachGame() throws Exception {
        FeedbackService fooService = mock(FeedbackService.class);
        FeedbackService barService = mock(FeedbackService.class);

        services.put("FOO", fooService);
        services.put("BAR", barService);

        task.go();
        verify(fooService).readFeedback();
        verify(barService).readFeedback();
    }

}
