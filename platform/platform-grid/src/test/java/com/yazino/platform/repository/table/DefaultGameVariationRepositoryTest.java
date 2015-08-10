package com.yazino.platform.repository.table;

import com.yazino.platform.model.table.Table;
import com.yazino.platform.persistence.table.JDBCGameVariationDAO;
import com.yazino.platform.table.GameVariation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class DefaultGameVariationRepositoryTest {

    @Mock
    private JDBCGameVariationDAO gameVariationDAO;

    private GameVariationRepository underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new DefaultGameVariationRepository(gameVariationDAO);
    }

    @Test
    public void refreshingTemplatesReadsThemFromTheDatabaseAndRecordsThemInternally() {
        when(gameVariationDAO.retrieveAll()).thenReturn(asList(aVariation(-1, 2), aVariation(-2, 3)));

        underTest.refreshAll();

        assertThat(underTest.findById(BigDecimal.valueOf(-1)), is(equalTo(aVariation(-1, 2))));
        assertThat(underTest.findById(BigDecimal.valueOf(-2)), is(equalTo(aVariation(-2, 3))));
    }

    @Test
    public void findByIdReturnsNullWhenNoMatchingTemplatesIsPresentInTheSpace() {
        final GameVariation GameVariation = underTest.findById(BigDecimal.valueOf(-120));

        assertThat(GameVariation, is(nullValue()));
    }

    @Test
    public void findByIdReturnsTheMatchingTemplateWhenPresent() {
        when(gameVariationDAO.retrieveAll()).thenReturn(asList(aVariation(-120, 2)));
        underTest.refreshAll();

        final GameVariation GameVariation = underTest.findById(BigDecimal.valueOf(-120));

        assertThat(GameVariation, is(equalTo(aVariation(-120, 2))));
    }

    @Test
    public void anIDCanBeFoundGivenTheNameAndGameType() {
        when(gameVariationDAO.retrieveAll()).thenReturn(asList(aVariation(-120, 2)));
        underTest.refreshAll();

        final BigDecimal id = underTest.getIdForName("template-120", "getGameType-120");

        assertThat(id, is(equalTo(BigDecimal.valueOf(-120))));
    }

    @Test
    public void aNullIDIsReturnedGivenANonExistentNameAndGameType() {
        final BigDecimal id = underTest.getIdForName("aName", "aGameType");

        assertThat(id, is(nullValue()));
    }

    @Test
    public void populatingPropertiesWillSetTheTemplateNameAndPropertiesOnTheTable() {
        final GameVariation template = aVariation(-130, 1);
        when(gameVariationDAO.retrieveAll()).thenReturn(asList(template));
        underTest.refreshAll();
        final Table table = new Table();
        table.setTemplateId(BigDecimal.valueOf(-130));

        underTest.populateProperties(table);

        assertThat(table.getTemplateName(), is(equalTo("template-130")));
        assertThat(table.getVariationProperties(), is(equalTo(template.getProperties())));
    }

    @Test(expected = IllegalStateException.class)
    public void populatingPropertiesWillThrowAnIllegalStateExceptionIfTheTableHasAnInvalidTemplateId() {
        final Table table = new Table();
        table.setTemplateId(BigDecimal.valueOf(-130));
        underTest.populateProperties(table);
    }

    @Test
    public void templatesAreLazyLoadedOnTheFirstQueryIfNotPresent() {
        when(gameVariationDAO.retrieveAll()).thenReturn(asList(aVariation(-1, 2)));

        assertThat(underTest.findById(BigDecimal.valueOf(-1)), is(equalTo(aVariation(-1, 2))));
    }

    @Test
    public void templatesAreNotLoadedAgainOnAQueryIfPresent() {
        when(gameVariationDAO.retrieveAll()).thenReturn(asList(aVariation(-1, 2)));
        underTest.refreshAll();

        underTest.findById(BigDecimal.valueOf(-1));

        verify(gameVariationDAO, times(1)).retrieveAll();
    }

    @Test
    public void allTemplatesCanBeRetrievedForAGameType() {
        GameVariation gameVariation1 = new GameVariation(new BigDecimal(1), "aGameType1", "aTemplate", new HashMap<String, String>());
        GameVariation gameVariation2 = new GameVariation(new BigDecimal(2), "aGameType2", "aTemplate", new HashMap<String, String>());
        GameVariation gameVariation3 = new GameVariation(new BigDecimal(3), "aGameType1", "aTemplate", new HashMap<String, String>());
        when(gameVariationDAO.retrieveAll()).thenReturn(asList(gameVariation1, gameVariation2, gameVariation3));

        Set<GameVariation> actualGameTypes = underTest.variationsFor("aGameType1");

        assertThat(actualGameTypes, is(equalTo((Set<GameVariation>) newHashSet(gameVariation1, gameVariation3))));
    }

    @Test
    public void allTemplatesForANonExistentGameTypesReturnsAnEmptySet() {
        when(gameVariationDAO.retrieveAll()).thenReturn(new ArrayList<GameVariation>());

        Set<GameVariation> actualGameTypes = underTest.variationsFor("aGameType1");

        assertThat(actualGameTypes, is(not(nullValue())));
        assertThat(actualGameTypes.isEmpty(), is(true));
    }

    private GameVariation aVariation(final int id,
                                     final int numberOfProperties) {
        final HashMap<String, String> properties = new HashMap<String, String>();
        for (int i = 0; i < numberOfProperties; ++i) {
            properties.put("property" + i, "value" + i);
        }
        return new GameVariation(new BigDecimal(id), "getGameType" + id, "template" + id, properties);
    }

}
