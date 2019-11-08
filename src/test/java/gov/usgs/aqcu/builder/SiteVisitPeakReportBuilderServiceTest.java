package gov.usgs.aqcu.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.any;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.TimeSeriesDataServiceResponse;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.TimeSeriesDescription;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.TimeSeriesPoint;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.Qualifier;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.Reading;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.ReadingType;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.StatisticalDateTimeOffset;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.LocationDescription;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.DoubleWithDisplay;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.FieldVisitDescription;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.FieldVisitDataServiceResponse;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.InspectionActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import gov.usgs.aqcu.model.SVPReportMetadata;
import gov.usgs.aqcu.model.FieldVisitReading;
import gov.usgs.aqcu.model.SiteVisitPeakReport;
import gov.usgs.aqcu.parameter.SiteVisitPeakRequestParameters;
import gov.usgs.aqcu.retrieval.FieldVisitDataService;
import gov.usgs.aqcu.retrieval.FieldVisitDescriptionService;
import gov.usgs.aqcu.retrieval.LocationDescriptionListService;
import gov.usgs.aqcu.retrieval.QualifierLookupService;
import gov.usgs.aqcu.retrieval.TimeSeriesDataService;
import gov.usgs.aqcu.retrieval.TimeSeriesDescriptionListService;
import gov.usgs.aqcu.util.AqcuTimeUtils;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class SiteVisitPeakReportBuilderServiceTest {
	@MockBean
	private TimeSeriesDescriptionListService timeSeriesDescriptionListService;
	@MockBean
	private FieldVisitDescriptionService fieldVisitDescriptionService;
	@MockBean
	private FieldVisitDataService fieldVisitDataService;
	@MockBean
	private LocationDescriptionListService locationDescriptionListService;
	@MockBean
	private TimeSeriesDataService timeSeriesDataService;
	@MockBean
	private QualifierLookupService qualifierLookupService;
	
	private FieldVisitReadingsBuilderService readingsService;	
	private SiteVisitPeakReportBuilderService service;

	@Before
	public void setup() {
		readingsService = new FieldVisitReadingsBuilderService();
		service = new SiteVisitPeakReportBuilderService(timeSeriesDescriptionListService, readingsService, fieldVisitDescriptionService, fieldVisitDataService, locationDescriptionListService, timeSeriesDataService, qualifierLookupService);
	}

	@Test
	public void getMetadataTest() {
		given(locationDescriptionListService.getByLocationIdentifier(any(String.class))).willReturn(new LocationDescription()
			.setIdentifier("location").setName("location-name")
		);
		SiteVisitPeakRequestParameters params = new SiteVisitPeakRequestParameters();
		params.setExcludeComments(true);
		params.setStartDate(LocalDate.parse("2018-01-01"));
		params.setEndDate(LocalDate.parse("2018-02-01"));
		TimeSeriesDescription desc = new TimeSeriesDescription();
		desc.setIdentifier("desc");
		desc.setLocationIdentifier("location");
		desc.setUtcOffset(0.0D);
		TimeSeriesDataServiceResponse tsData = new TimeSeriesDataServiceResponse();
		tsData.setQualifiers(new ArrayList<>(Arrays.asList(new Qualifier())));
		SVPReportMetadata result = service.getMetadata(params, desc, tsData);
		assertEquals(result.getStartDate(), params.getStartInstant(ZoneOffset.UTC));
		assertEquals(result.getEndDate(), params.getEndInstant(ZoneOffset.UTC));
		assertEquals(result.getQualifierMetadata().isEmpty(), true);
		assertEquals(result.getStationId(), "location");
		assertEquals(result.getStationName(), "location-name");
		assertEquals(result.getTimeseriesLabel(), "desc");
		assertEquals(result.getTimezone(), AqcuTimeUtils.getTimezone(0.0D));
		assertEquals(result.getTitle(), SiteVisitPeakReportBuilderService.REPORT_TITLE);
	}

	@Test
	public void getQualifiersBetweenDatesTest() {
		Qualifier q1 = new Qualifier();
		q1.setStartTime(Instant.parse("2018-03-15T00:00:00Z"));
		q1.setEndTime(Instant.parse("2018-03-20T00:00:00Z"));
		Qualifier q2 = new Qualifier();
		q2.setStartTime(Instant.parse("2018-03-15T00:00:00Z"));
		q2.setEndTime(Instant.parse("2018-03-15T23:59:59.999999Z"));
		Qualifier q3 = new Qualifier();
		q3.setStartTime(Instant.parse("2018-03-20T00:00:00Z"));
		q3.setEndTime(Instant.parse("2018-03-21T00:00:00Z"));
		Qualifier q4 = new Qualifier();
		q4.setStartTime(Instant.parse("2018-03-12T00:00:00Z"));
		q4.setEndTime(Instant.parse("2018-03-15T00:01:00Z"));
		Qualifier q5 = new Qualifier();
		q5.setStartTime(Instant.parse("2018-03-19T00:00:00Z"));
		q5.setEndTime(Instant.parse("2018-03-22T00:00:00Z"));
		Qualifier q6 = new Qualifier();
		q6.setStartTime(Instant.parse("2018-03-01T00:00:00Z"));
		q6.setEndTime(Instant.parse("2018-03-25T00:00:00Z"));
		Qualifier q7 = new Qualifier();
		q7.setStartTime(Instant.parse("2018-03-01T00:00:00Z"));
		q7.setEndTime(Instant.parse("2018-03-14T00:00:00Z"));
		Qualifier q8 = new Qualifier();
		q8.setStartTime(Instant.parse("2018-03-21T00:00:00Z"));
		q8.setEndTime(Instant.parse("2018-03-25T00:00:00Z"));
		List<Qualifier> quals = Arrays.asList(q1,q2,q3,q4,q5,q6,q7,q8);
		List<Qualifier> result = service.getQualifiersBetweenDates(Instant.parse("2018-03-15T00:00:00Z"), Instant.parse("2018-03-20T00:00:00Z"), quals);
		assertEquals(result.size(), 5);
		assertThat(result, containsInAnyOrder(q1,q2,q4,q5,q6));
	}

	@Test
	public void getPointsBetweenDatesTest() {
		TimeSeriesPoint p1 = new TimeSeriesPoint();
		p1.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-03-15T00:00:00Z")));
		TimeSeriesPoint p2 = new TimeSeriesPoint();
		p2.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-03-15T12:00:00Z")));
		TimeSeriesPoint p3 = new TimeSeriesPoint();
		p3.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-03-16T00:00:00Z")));
		TimeSeriesPoint p4 = new TimeSeriesPoint();
		p4.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-03-16T12:00:00Z")));
		TimeSeriesPoint p5 = new TimeSeriesPoint();
		p5.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-03-17T00:00:00Z")));
		TimeSeriesPoint p6 = new TimeSeriesPoint();
		p6.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-03-17T12:00:00Z")));
		TimeSeriesPoint p7 = new TimeSeriesPoint();
		p7.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-03-18T00:00:00Z")));
		List<TimeSeriesPoint> points = Arrays.asList(p1,p2,p3,p4,p5,p6,p7);
		List<TimeSeriesPoint> result = service.getPointsBetweenDates(Instant.parse("2018-03-15T12:00:00Z"), Instant.parse("2018-03-17T12:00:00Z"), points);
		assertEquals(result.size(), 4);
		assertThat(result, containsInAnyOrder(p2,p3,p4,p5));
	}

	@Test
	public void addAssociatedIvDataToReadingEmptyTest() {
		TimeSeriesDataServiceResponse tsData = new TimeSeriesDataServiceResponse()
			.setPoints(new ArrayList<>())
			.setQualifiers(new ArrayList<>());
		
		FieldVisitReading sr = new FieldVisitReading(Instant.parse("2018-02-03T12:00:00Z"), "test", "test", Arrays.asList("test"), new Reading());
		sr.setLastVisitPrior(Instant.parse("2018-02-01T00:00:00Z"));
		FieldVisitReading result = service.addAssociatedIvDataToReading(sr, tsData);
		assertEquals(result.getAssociatedIvQualifiers().size(), 0);
		assertNull(result.getAssociatedIvTime());
		assertNull(result.getAssociatedIvValue());
	}

	@Test
	public void addAssociatedIvDataToReadingTest() {
		TimeSeriesDataServiceResponse tsData = new TimeSeriesDataServiceResponse()
			.setPoints(getTimeSeriesPoints())
			.setQualifiers(getQualifiers());
		
		FieldVisitReading sr = new FieldVisitReading(Instant.parse("2018-02-03T12:00:00Z"), "test", "test", Arrays.asList("test"), new Reading());
		sr.setLastVisitPrior(Instant.parse("2018-02-01T00:00:00Z"));
		FieldVisitReading result = service.addAssociatedIvDataToReading(sr, tsData);
		assertEquals(result.getAssociatedIvQualifiers().size(), 2);
		assertEquals(result.getAssociatedIvTime(), Instant.parse("2018-02-02T12:00:00Z"));
		assertEquals(result.getAssociatedIvValue(), "3.0");

		sr = new FieldVisitReading(Instant.parse("2018-02-02T12:00:00Z"), "test", "test", Arrays.asList("test"), new Reading());
		sr.setLastVisitPrior(Instant.parse("2018-02-01T00:00:00Z"));
		result = service.addAssociatedIvDataToReading(sr, tsData);
		assertEquals(result.getAssociatedIvQualifiers().size(), 1);
		assertEquals(result.getAssociatedIvTime(), Instant.parse("2018-02-02T00:00:00Z"));
		assertEquals(result.getAssociatedIvValue(), "3.0");

		sr = new FieldVisitReading(Instant.parse("2018-02-01T12:00:00Z"), "test", "test", Arrays.asList("test"), new Reading());
		sr.setLastVisitPrior(Instant.parse("2018-02-01T00:00:00Z"));
		result = service.addAssociatedIvDataToReading(sr, tsData);
		assertEquals(result.getAssociatedIvQualifiers().size(), 1);
		assertEquals(result.getAssociatedIvTime(), Instant.parse("2018-02-01T00:00:00Z"));
		assertEquals(result.getAssociatedIvValue(), "1.0");

		sr = new FieldVisitReading(Instant.parse("2018-02-02T00:00:00Z"), "test", "test", Arrays.asList("test"), new Reading());
		sr.setLastVisitPrior(Instant.parse("2018-02-01T12:00:00Z"));
		result = service.addAssociatedIvDataToReading(sr, tsData);
		assertEquals(result.getAssociatedIvQualifiers().size(), 1);
		assertEquals(result.getAssociatedIvTime(), Instant.parse("2018-02-01T12:00:00Z"));
		assertEquals(result.getAssociatedIvValue(), "2.0");

		sr = new FieldVisitReading(Instant.parse("2018-02-03T00:00:00Z"), "test", "test", Arrays.asList("test"), new Reading());
		sr.setLastVisitPrior(Instant.parse("2018-02-02T12:00:00Z"));
		result = service.addAssociatedIvDataToReading(sr, tsData);
		assertEquals(result.getAssociatedIvQualifiers().size(), 0);
		assertEquals(result.getAssociatedIvTime(), Instant.parse("2018-02-02T12:00:00Z"));
		assertEquals(result.getAssociatedIvValue(), "3.0");
	}

	@Test
	public void getFieldVisitReadingsTest() {
		given(fieldVisitDescriptionService.getDescriptions(any(String.class), any(ZoneOffset.class), any(SiteVisitPeakRequestParameters.class))).willReturn(
			getVisits()
		);
		given(fieldVisitDataService.get("visit-1")).willReturn(
			new FieldVisitDataServiceResponse()
				.setInspectionActivity(new InspectionActivity().setReadings(getReadings(0)).setParty("party-1"))
		);
		given(fieldVisitDataService.get("visit-2")).willReturn(
			new FieldVisitDataServiceResponse()
				.setInspectionActivity(new InspectionActivity().setReadings(getReadings(2)).setParty("party-2"))
		);
		TimeSeriesDataServiceResponse tsData = new TimeSeriesDataServiceResponse()
			.setPoints(getTimeSeriesPoints())
			.setQualifiers(getQualifiers());

		List<FieldVisitReading> result = service.getFieldVisitReadings("location", ZoneOffset.UTC, new SiteVisitPeakRequestParameters(), tsData);
		assertEquals(result.size(), 4);
		assertNull(result.get(0).getLastVisitPrior());
		assertNull(result.get(1).getLastVisitPrior());
		assertNotNull(result.get(2).getLastVisitPrior());
		assertNotNull(result.get(3).getLastVisitPrior());
		assertEquals(result.get(0).getComments().get(0), "test-ExtremeMax");
		assertEquals(result.get(0).getParty(), "party-1");
		assertEquals(result.get(0).getVisitTime(), Instant.parse("2018-02-01T00:00:00Z"));
		assertEquals(result.get(0).getTime(), Instant.parse("2018-02-01T02:00:00Z"));
		assertEquals(result.get(1).getComments().get(0), "test-ExtremeMax");
		assertEquals(result.get(1).getParty(), "party-1");
		assertEquals(result.get(1).getVisitTime(), Instant.parse("2018-02-01T00:00:00Z"));
		assertEquals(result.get(1).getTime(), Instant.parse("2018-02-01T06:00:00Z"));
		assertEquals(result.get(2).getComments().get(0), "test-ExtremeMax");
		assertEquals(result.get(2).getParty(), "party-2");
		assertEquals(result.get(2).getVisitTime(), Instant.parse("2018-02-02T00:00:00Z"));
		assertEquals(result.get(2).getTime(), Instant.parse("2018-02-03T02:00:00Z"));
		assertEquals(result.get(3).getComments().get(0), "test-ExtremeMax");
		assertEquals(result.get(3).getParty(), "party-2");
		assertEquals(result.get(3).getVisitTime(), Instant.parse("2018-02-02T00:00:00Z"));
		assertEquals(result.get(3).getTime(), Instant.parse("2018-02-03T06:00:00Z"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void buildReportTest() {
		given(timeSeriesDescriptionListService.getTimeSeriesDescription(any(String.class))).willReturn(
			new TimeSeriesDescription()
				.setIdentifier("test-id")
				.setUtcOffset(0.0D)
				.setLocationIdentifier("location-id")
		);
		given(timeSeriesDataService.get(any(String.class), any(SiteVisitPeakRequestParameters.class), eq(ZoneOffset.UTC), eq(false), eq(false), eq(false), eq(null))).willReturn(
			new TimeSeriesDataServiceResponse()
				.setPoints(getTimeSeriesPoints())
				.setQualifiers(getQualifiers())
		);
		given(fieldVisitDescriptionService.getDescriptions(any(String.class), any(ZoneOffset.class), any(SiteVisitPeakRequestParameters.class))).willReturn(
			getVisits()
		);
		given(locationDescriptionListService.getByLocationIdentifier(any(String.class))).willReturn(
			new LocationDescription()
				.setIdentifier("location-id")
				.setName("location-name")
		);
		given(qualifierLookupService.getByQualifierList(any(List.class))).willReturn(
			new HashMap<>()
		);
		given(fieldVisitDataService.get("visit-1")).willReturn(
			new FieldVisitDataServiceResponse()
				.setInspectionActivity(new InspectionActivity().setReadings(getReadings(0)).setParty("party-1"))
		);
		given(fieldVisitDataService.get("visit-2")).willReturn(
			new FieldVisitDataServiceResponse()
				.setInspectionActivity(new InspectionActivity().setReadings(getReadings(2)).setParty("party-2"))
		);
		SiteVisitPeakRequestParameters params = new SiteVisitPeakRequestParameters();
		params.setStartDate(LocalDate.parse("2018-02-01"));
		params.setEndDate(LocalDate.parse("2018-02-03"));
		params.setPrimaryTimeseriesIdentifier("test-id");

		SiteVisitPeakReport result = service.buildReport(params, "test-user");
		assertEquals(result.getReadings().size(), 4);
		assertEquals(result.getReportMetadata().getStartDate(), Instant.parse("2018-02-01T00:00:00Z"));
		assertEquals(result.getReportMetadata().getEndDate(), Instant.parse("2018-02-03T23:59:59.999999999Z"));
		assertTrue(result.getReportMetadata().getQualifierMetadata().isEmpty());
	}

	protected ArrayList<FieldVisitDescription> getVisits() {
		return new ArrayList<>(Arrays.asList(
			new FieldVisitDescription()
				.setStartTime(Instant.parse("2018-02-01T00:00:00Z"))
				.setIdentifier("visit-1"),
			new FieldVisitDescription()
				.setStartTime(Instant.parse("2018-02-02T00:00:00Z"))
				.setIdentifier("visit-2")
		));
	}

	protected ArrayList<Reading> getReadings(Integer dayOffset) {
		dayOffset = dayOffset == null ? 0 : dayOffset;
		Reading r1 = new Reading()
			.setReadingType(ReadingType.ExtremeMax)
			.setComments("test-ExtremeMax")
			.setMonitoringMethod("method-1")
			.setValue(new DoubleWithDisplay().setNumeric(1.0D).setDisplay("1.0"))
			.setTime(Instant.parse("2018-02-01T02:00:00Z").plus(dayOffset, ChronoUnit.DAYS));
		Reading r2 = new Reading()
			.setReadingType(ReadingType.Reference)
			.setComments("test-Reference")
			.setMonitoringMethod("method-1")
			.setValue(new DoubleWithDisplay().setNumeric(2.0D).setDisplay("2.0"))
			.setTime(Instant.parse("2018-02-01T05:00:00Z").plus(dayOffset, ChronoUnit.DAYS));
		Reading r3 = new Reading()
			.setReadingType(ReadingType.ExtremeMax)
			.setComments("test-ExtremeMax")
			.setMonitoringMethod("method-2")
			.setValue(new DoubleWithDisplay().setNumeric(3.0D).setDisplay("3.0"))
			.setTime(Instant.parse("2018-02-01T06:00:00Z").plus(dayOffset, ChronoUnit.DAYS));
		return new ArrayList<>(Arrays.asList(r1,r2,r3));
	}

	protected ArrayList<TimeSeriesPoint> getTimeSeriesPoints() {
		TimeSeriesPoint p1 = new TimeSeriesPoint();
		p1.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-02-01T00:00:00Z")));
		p1.setValue(new DoubleWithDisplay().setNumeric(1.0D).setDisplay("1.0"));
		TimeSeriesPoint p2 = new TimeSeriesPoint();
		p2.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-02-01T12:00:00Z")));
		p2.setValue(new DoubleWithDisplay().setNumeric(2.0D).setDisplay("2.0"));
		TimeSeriesPoint p3 = new TimeSeriesPoint();
		p3.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-02-02T00:00:00Z")));
		p3.setValue(new DoubleWithDisplay().setNumeric(3.0D).setDisplay("3.0"));
		TimeSeriesPoint p4 = new TimeSeriesPoint();
		p4.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-02-02T12:00:00Z")));
		p4.setValue(new DoubleWithDisplay().setNumeric(3.0D).setDisplay("3.0"));
		TimeSeriesPoint p5 = new TimeSeriesPoint();
		p5.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-02-03T00:00:00Z")));
		p5.setValue(new DoubleWithDisplay().setNumeric(2.0D).setDisplay("2.0"));
		TimeSeriesPoint p6 = new TimeSeriesPoint();
		p6.setTimestamp(new StatisticalDateTimeOffset().setDateTimeOffset(Instant.parse("2018-02-03T12:00:00Z")));
		p6.setValue(new DoubleWithDisplay().setNumeric(1.0D).setDisplay("1.0"));
		return new ArrayList<>(Arrays.asList(p1,p2,p3,p4,p5,p6));
	}

	protected ArrayList<Qualifier> getQualifiers() {
		Qualifier q1 = new Qualifier();
		q1.setStartTime(Instant.parse("2018-02-01T00:00:00Z"));
		q1.setEndTime(Instant.parse("2018-02-02T12:00:00Z"));
		q1.setIdentifier("q1");
		Qualifier q2 = new Qualifier();
		q2.setStartTime(Instant.parse("2018-02-03T00:00:00Z"));
		q2.setEndTime(Instant.parse("2018-02-03T12:00:00Z"));
		q2.setIdentifier("q2");
		return new ArrayList<>(Arrays.asList(q1,q2));
	}
}