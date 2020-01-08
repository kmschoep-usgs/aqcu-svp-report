package gov.usgs.aqcu.calc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.ReadingType;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import gov.usgs.aqcu.model.FieldVisitReading;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class LastValidVisitCalculatorTest {
    FieldVisitReading r1 = new FieldVisitReading(
        Instant.parse("2019-01-01T00:00:00Z"), "party1", "status1", Arrays.asList("comment1"), 
        Instant.parse("2019-01-01T00:01:00Z"), "mon1", "1", "1", "sub1", ReadingType.ExtremeMax
    );
    FieldVisitReading r2 = new FieldVisitReading(
        Instant.parse("2019-01-01T00:00:00Z"), "party1", "status1", Arrays.asList("comment2"), 
        Instant.parse("2019-01-01T00:02:00Z"), "mon1", "1", "1.1", "sub1", ReadingType.ExtremeMax
    );
    FieldVisitReading r3 = new FieldVisitReading(
        Instant.parse("2019-02-01T00:00:00Z"), "party2", "status2", Arrays.asList("comment3"), 
        Instant.parse("2019-02-01T00:01:00Z"), "mon1", "1", "2", "sub1", ReadingType.ExtremeMax
    );
    FieldVisitReading r4 = new FieldVisitReading(
        Instant.parse("2019-02-01T00:00:00Z"), "party2", "status2", Arrays.asList("comment4"), 
        Instant.parse("2019-02-01T00:02:00Z"), "mon1", "1", "2.1", "sub1", ReadingType.ExtremeMax
    );
    FieldVisitReading r5 = new FieldVisitReading(
        Instant.parse("2019-02-01T00:00:00Z"), "party2", "status2", Arrays.asList("comment5"), 
        Instant.parse("2019-02-01T00:03:00Z"), "mon1", "1", "not read", "sub1", ReadingType.ExtremeMax
    );
    FieldVisitReading r6 = new FieldVisitReading(
        Instant.parse("2019-03-01T00:00:00Z"), "party3", "status3", Arrays.asList("comment6"), 
        Instant.parse("2019-03-01T00:01:00Z"), "mon1", "1", "no mark", "sub1", ReadingType.ExtremeMax
    );
    FieldVisitReading r7 = new FieldVisitReading(
        Instant.parse("2019-04-01T00:00:00Z"), "party4", "status4", Arrays.asList("comment7"), 
        Instant.parse("2019-04-01T00:01:00Z"), "mon1", "1", "not read", "sub1", ReadingType.ExtremeMax
    );
    FieldVisitReading r8 = new FieldVisitReading(
        Instant.parse("2019-05-01T00:00:00Z"), "party5", "status5", Arrays.asList("comment8"), 
        Instant.parse("2019-05-01T00:01:00Z"), "mon1", "1", "4", "sub1", ReadingType.ExtremeMax
    );
    FieldVisitReading r9 = new FieldVisitReading(
        Instant.parse("2019-05-01T00:00:00Z"), "party6", "status6", Arrays.asList("comment9"), 
        Instant.parse("2019-05-01T00:01:00Z"), "mon1", "1", "6", "sub1", ReadingType.ExtremeMax
    );
    
    @Test
    public void happyPathTest() {
        List<Pair<String, FieldVisitReading>> pairs = Arrays.asList(
            new ImmutablePair<>("v1", r1),
            new ImmutablePair<>("v1", r2),
            new ImmutablePair<>("v2", r3),
            new ImmutablePair<>("v2", r4),
            new ImmutablePair<>("v2", r5),
            new ImmutablePair<>("v3", r6),
            new ImmutablePair<>("v4", r7),
            new ImmutablePair<>("v5", r8),
            new ImmutablePair<>("v6", r9)
        );

        List<FieldVisitReading> results = new LastValidVisitCalculator().fill(pairs);
        assertEquals(9, results.size());

        assertEquals("comment1", results.get(0).getComments().get(0));
        assertNull(results.get(0).getLastVisitPrior());
        assertEquals("comment2", results.get(1).getComments().get(0));
        assertEquals(Instant.parse("2019-01-01T00:00:00Z"), results.get(1).getLastVisitPrior());
        assertEquals("comment3", results.get(2).getComments().get(0));
        assertEquals(Instant.parse("2019-01-01T00:00:00Z"), results.get(2).getLastVisitPrior());
        assertEquals("comment4", results.get(3).getComments().get(0));
        assertEquals(Instant.parse("2019-02-01T00:00:00Z"), results.get(3).getLastVisitPrior());
        assertEquals("comment5", results.get(4).getComments().get(0));
        assertNull(results.get(4).getLastVisitPrior());
        assertEquals("comment6", results.get(5).getComments().get(0));
        assertEquals(Instant.parse("2019-02-01T00:00:00Z"), results.get(5).getLastVisitPrior());
        assertEquals("comment7", results.get(6).getComments().get(0));
        assertNull(results.get(6).getLastVisitPrior());
        assertEquals("comment8", results.get(7).getComments().get(0));
        assertEquals(Instant.parse("2019-03-01T00:00:00Z"), results.get(7).getLastVisitPrior());
        assertEquals("comment9", results.get(8).getComments().get(0));
        assertEquals(Instant.parse("2019-05-01T00:00:00Z"), results.get(8).getLastVisitPrior());
    }

    @Test
    public void sortTest() {
        List<Pair<String, FieldVisitReading>> pairs = Arrays.asList(
            new ImmutablePair<>("v1", r1),
            new ImmutablePair<>("v2", r6),
            new ImmutablePair<>("v3", r2),
            new ImmutablePair<>("v4", null),
            new ImmutablePair<>("v5", r4),
            new ImmutablePair<>("v6", null),
            new ImmutablePair<>("v7", null),
            new ImmutablePair<>("v8", r5),
            new ImmutablePair<>("v9", r3),
            new ImmutablePair<>("v10", null),
            new ImmutablePair<>("v11", null),
            new ImmutablePair<>("v12", null),
            new ImmutablePair<>("v13", r8),
            new ImmutablePair<>("v14", r7),
            new ImmutablePair<>("v15", null),
            new ImmutablePair<>("v16", r9)
        );

        Collections.sort(pairs, new LastValidVisitCalculator().VISIT_READING_COMPARATOR);

        assertEquals("v4", pairs.get(0).getLeft());
        assertEquals("v6", pairs.get(1).getLeft());
        assertEquals("v7", pairs.get(2).getLeft());
        assertEquals("v10", pairs.get(3).getLeft());
        assertEquals("v11", pairs.get(4).getLeft());
        assertEquals("v12", pairs.get(5).getLeft());
        assertEquals("v15", pairs.get(6).getLeft());
        assertEquals("v1", pairs.get(7).getLeft());
        assertEquals("v3", pairs.get(8).getLeft());
        assertEquals("v9", pairs.get(9).getLeft());
        assertEquals("v5", pairs.get(10).getLeft());
        assertEquals("v8", pairs.get(11).getLeft());
        assertEquals("v2", pairs.get(12).getLeft());
        assertEquals("v14", pairs.get(13).getLeft());
        assertEquals("v13", pairs.get(14).getLeft());
        assertEquals("v16", pairs.get(15).getLeft());
    }
}