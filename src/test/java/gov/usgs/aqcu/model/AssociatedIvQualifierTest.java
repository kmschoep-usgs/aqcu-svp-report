package gov.usgs.aqcu.model;

import static org.junit.Assert.assertEquals;

import java.time.Instant;

import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.Qualifier;

import org.junit.Test;

public class AssociatedIvQualifierTest {

	@Test
	public void constructorTest() {
		Qualifier q = new Qualifier();
		q.setStartTime(Instant.parse("2018-02-01T00:00:00Z"));
		q.setEndTime(Instant.parse("2018-02-02T12:00:00Z"));
		q.setIdentifier("q");
		
		AssociatedIvQualifier aQ = new AssociatedIvQualifier(q);
		assertEquals(q.getIdentifier(), aQ.getIdentifier());
		assertEquals(q.getStartTime(), aQ.getStartDate());
		assertEquals(q.getEndTime(), aQ.getEndDate());
	}
}