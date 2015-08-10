package com.yazino.web.data;

import com.yazino.platform.community.PlayerService;
import com.yazino.web.domain.DefaultPicture;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class PictureRepositoryIntegrationTest {

    @Autowired
    private PictureRepository pictureRepository;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private DefaultPicture defaultPicture;

    @DirtiesContext
    @Test
    public void testPictureCaching() throws IOException {
        when(playerService.getPictureUrl(BigDecimal.ONE)).thenReturn("picture1");

        for (int i = 0; i < 5; i++) {
            Assert.assertEquals("picture1", pictureRepository.getPicture(BigDecimal.ONE));
        }

        verify(playerService, times(1)).getPictureUrl(BigDecimal.ONE);
    }

    @DirtiesContext
    @Test
    public void returnFallbackIfServiceReturnsNull() throws Exception {
        when(playerService.getPictureUrl(BigDecimal.TEN)).thenReturn(null);

        assertEquals(defaultPicture.getUrl(), pictureRepository.getPicture(BigDecimal.TEN));
    }

    @DirtiesContext
    @Test
    public void returnFallbackIfServiceBreaks() throws Exception {
        when(playerService.getPictureUrl(BigDecimal.valueOf(3))).thenThrow(new RuntimeException("error getting picture URL"));

        assertEquals(defaultPicture.getUrl(), pictureRepository.getPicture(BigDecimal.valueOf(3)));
    }
}
