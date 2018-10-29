package gov.usgs.aqcu.builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.FieldVisitDataServiceResponse;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.FieldVisitDescription;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.InspectionActivity;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.Qualifier;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.ReadingType;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.TimeSeriesDataServiceResponse;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.TimeSeriesDescription;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.TimeSeriesPoint;

import gov.usgs.aqcu.calc.LastValidVisitCalculator;
import gov.usgs.aqcu.model.AssociatedIvQualifier;
import gov.usgs.aqcu.model.MinMaxData;
import gov.usgs.aqcu.model.MinMaxPoint;
import gov.usgs.aqcu.model.SVPReportMetadata;
import gov.usgs.aqcu.model.SVPReportReading;
import gov.usgs.aqcu.model.SiteVisitPeakReport;
import gov.usgs.aqcu.parameter.SiteVisitPeakRequestParameters;
import gov.usgs.aqcu.retrieval.FieldVisitDataService;
import gov.usgs.aqcu.retrieval.FieldVisitDescriptionService;
import gov.usgs.aqcu.retrieval.LocationDescriptionListService;
import gov.usgs.aqcu.retrieval.QualifierLookupService;
import gov.usgs.aqcu.retrieval.TimeSeriesDataService;
import gov.usgs.aqcu.retrieval.TimeSeriesDescriptionListService;
import gov.usgs.aqcu.util.AqcuTimeUtils;
import gov.usgs.aqcu.util.BigDecimalSummaryStatistics;
import gov.usgs.aqcu.util.DoubleWithDisplayUtil;
import gov.usgs.aqcu.util.TimeSeriesUtils;

@Service
public class SiteVisitPeakReportBuilderService {
	public static final String REPORT_TITLE = "Site Visit Peak";
	public static final String REPORT_TYPE = "siteVisitPeak";

	private TimeSeriesDescriptionListService timeSeriesDescriptionListService;
	private FieldVisitDescriptionService fieldVisitDescriptionService;
	private FieldVisitDataService fieldVisitDataService;
	private LocationDescriptionListService locationDescriptionListService;
	private TimeSeriesDataService timeSeriesDataService;
	private QualifierLookupService qualifierLookupService;

	@Autowired
	public SiteVisitPeakReportBuilderService(
			TimeSeriesDescriptionListService timeSeriesDescriptionListService,
			FieldVisitDescriptionService fieldVisitDescriptionService,
			FieldVisitDataService fieldVisitDataService,
			LocationDescriptionListService locationDescriptionListService,
			TimeSeriesDataService timeSeriesDataService,
			QualifierLookupService qualifierLookupService) {
		this.timeSeriesDescriptionListService = timeSeriesDescriptionListService;
		this.fieldVisitDescriptionService = fieldVisitDescriptionService;
		this.fieldVisitDataService = fieldVisitDataService;
		this.locationDescriptionListService = locationDescriptionListService;
		this.timeSeriesDataService = timeSeriesDataService;
		this.qualifierLookupService = qualifierLookupService;
	}

	public SiteVisitPeakReport buildReport(SiteVisitPeakRequestParameters requestParameters, String requestingUser) {
		SiteVisitPeakReport report = new SiteVisitPeakReport();
		
		TimeSeriesDescription primaryDescription = timeSeriesDescriptionListService.getTimeSeriesDescription(requestParameters.getPrimaryTimeseriesIdentifier());
		String locationIdentifier = primaryDescription.getLocationIdentifier();
		ZoneOffset zoneOffset = TimeSeriesUtils.getZoneOffset(primaryDescription);
		List<SVPReportReading> readings = getFieldVisitReadings(locationIdentifier, zoneOffset, requestParameters);
		TimeSeriesDataServiceResponse primaryTsCorrected = timeSeriesDataService.get(requestParameters.getPrimaryTimeseriesIdentifier(), requestParameters, zoneOffset, false, false, false, null);
		SVPReportMetadata reportMetadata = getMetadata(requestParameters, primaryDescription, primaryTsCorrected);

		for (SVPReportReading reading : readings) {
			if (null != reading.getLastVisitPrior()) {
				List<TimeSeriesPoint> points = getPointsBetweenDates(reading.getLastVisitPrior(), reading.getVisitTime(), primaryTsCorrected.getPoints());
				List<AssociatedIvQualifier> qualifiers = getQualifiersBetweenDates(reading.getLastVisitPrior(), reading.getVisitTime(), primaryTsCorrected.getQualifiers())
					.stream().map(q -> new AssociatedIvQualifier(q)).collect(Collectors.toList());
				
				if(!qualifiers.isEmpty()) {
					reading.getAssociatedIvQualifiers().addAll(qualifiers);
				}
				
				MinMaxData minMaxData = getMinMaxData(points);
				MinMaxPoint minMaxPoint = minMaxData.getMax().get(minMaxData.getMax().size()-1);
				reading.setAssociatedIvTime(minMaxPoint.getTime());
				reading.setAssociatedIvValue(minMaxPoint.getValue().toPlainString());
			}
		}

		report.setReadings(readings);
		report.setReportMetadata(reportMetadata);
		return report;
	}

	protected List<SVPReportReading> getFieldVisitReadings(String locationIdentifier, ZoneOffset zoneOffset, SiteVisitPeakRequestParameters requestParameters) {
		List<SVPReportReading> readings = new ArrayList<>();

		// Process field visits
		for (FieldVisitDescription fieldVisitDescription : fieldVisitDescriptionService.getDescriptions(locationIdentifier, zoneOffset, requestParameters)) {
			FieldVisitDataServiceResponse fieldVisitDataServiceResponse = fieldVisitDataService.get(fieldVisitDescription.getIdentifier());
			InspectionActivity inspectionActivity = fieldVisitDataServiceResponse.getInspectionActivity();

			// Extract only ExtremeMax Readings
			if(inspectionActivity != null && inspectionActivity.getReadings() != null) {
				readings.addAll(inspectionActivity.getReadings().stream()
					.filter(r -> ReadingType.ExtremeMax.equals(r.getReadingType()))
					.map(r -> new SVPReportReading(fieldVisitDescription.getStartTime(), inspectionActivity.getParty(), r))
					.collect(Collectors.toList())
				);
			}
		}

		// Calculate last valid visit
		readings = new LastValidVisitCalculator().fill(readings);

		return readings;
	}

	/**
	 * This method should only be called if the timeSeriesPoints list is not null.
	 */
	protected MinMaxData getMinMaxData(List<TimeSeriesPoint> timeSeriesPoints) {
		Map<BigDecimal, List<MinMaxPoint>> minMaxPoints = timeSeriesPoints.parallelStream()
				.map(x -> {
					MinMaxPoint point = new MinMaxPoint(x.getTimestamp().getDateTimeOffset(), DoubleWithDisplayUtil.getRoundedValue(x.getValue()));
					return point;
				})
				.filter(x -> x.getValue() != null)
				.collect(Collectors.groupingByConcurrent(MinMaxPoint::getValue));

		BigDecimalSummaryStatistics stats = minMaxPoints.keySet().parallelStream()
				.collect(BigDecimalSummaryStatistics::new,
						BigDecimalSummaryStatistics::accept,
						BigDecimalSummaryStatistics::combine);

		return new MinMaxData(stats.getMin(), stats.getMax(), minMaxPoints);
	}

	protected List<TimeSeriesPoint> getPointsBetweenDates(Instant startDate, Instant endDate, List<TimeSeriesPoint> points) {
		if(points != null && !points.isEmpty()) {
			List<TimeSeriesPoint> filteredPoints = new ArrayList<>();
			for(TimeSeriesPoint point : points) {
				if(startDate.compareTo(point.getTimestamp().getDateTimeOffset()) <= 0 &&
					endDate.compareTo(point.getTimestamp().getDateTimeOffset()) > 0) 
				{
					filteredPoints.add(point);
				}
			}
			return filteredPoints;
		}

		return new ArrayList<>();
	}

	protected List<Qualifier> getQualifiersBetweenDates(Instant startDate, Instant endDate, List<Qualifier> qualifiers) {
		if(qualifiers != null && !qualifiers.isEmpty()) {
			List<Qualifier> filteredQualifiers = new ArrayList<>();
			for(Qualifier qual : qualifiers) {
				if(AqcuTimeUtils.doesTimeRangeOverlap(startDate, endDate, qual.getStartTime(), qual.getEndTime())) {
					filteredQualifiers.add(qual);
				}
			}
			return filteredQualifiers;
		}

		return new ArrayList<>();
	}

	protected SVPReportMetadata getMetadata(SiteVisitPeakRequestParameters requestParameters, TimeSeriesDescription primaryDescription, TimeSeriesDataServiceResponse primaryTsCorrected) {
		SVPReportMetadata metadata = new SVPReportMetadata();
		metadata.setTitle(REPORT_TITLE);
		metadata.setExcludeComments(requestParameters.getExcludedComments());
		metadata.setTimeseriesLabel(primaryDescription.getIdentifier());
		metadata.setTimezone(primaryDescription.getUtcOffset());
		metadata.setStartDate(requestParameters.getStartInstant(ZoneOffset.UTC));
		metadata.setEndDate(requestParameters.getEndInstant(ZoneOffset.UTC));
		metadata.setStationId(primaryDescription.getLocationIdentifier());
		metadata.setStationName(locationDescriptionListService.getByLocationIdentifier(primaryDescription.getLocationIdentifier()).getName());
		metadata.setQualifierMetadata(qualifierLookupService.getByQualifierList(primaryTsCorrected.getQualifiers()));
		return metadata;
	}
}