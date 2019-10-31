package gov.usgs.aqcu.builder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.FieldVisitDataServiceResponse;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.FieldVisitDescription;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.InspectionType;
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
import gov.usgs.aqcu.util.TimeSeriesUtils;
import gov.usgs.aqcu.util.LogExecutionTime;

@Service
public class SiteVisitPeakReportBuilderService {
	private static final Logger LOG = LoggerFactory.getLogger(SiteVisitPeakReportBuilderService.class);
		
	public static final String REPORT_TITLE = "Site Visit Peak";
	public static final String REPORT_TYPE = "siteVisitPeak";
	private static final List<String> includeInspections = Arrays.asList(InspectionType.CrestStageGage.name(), InspectionType.MaximumMinimumGage.name());

	private TimeSeriesDescriptionListService timeSeriesDescriptionListService;
	private FieldVisitReadingsBuilderService fieldVisitReadingsBuilderService;
	private FieldVisitDescriptionService fieldVisitDescriptionService;
	private FieldVisitDataService fieldVisitDataService;
	private LocationDescriptionListService locationDescriptionListService;
	private TimeSeriesDataService timeSeriesDataService;
	private QualifierLookupService qualifierLookupService;

	@Autowired
	public SiteVisitPeakReportBuilderService(
			TimeSeriesDescriptionListService timeSeriesDescriptionListService,
			FieldVisitReadingsBuilderService fieldVisitReadingsBuilderService,
			FieldVisitDescriptionService fieldVisitDescriptionService,
			FieldVisitDataService fieldVisitDataService,
			LocationDescriptionListService locationDescriptionListService,
			TimeSeriesDataService timeSeriesDataService,
			QualifierLookupService qualifierLookupService) {
		this.timeSeriesDescriptionListService = timeSeriesDescriptionListService;
		this.fieldVisitReadingsBuilderService = fieldVisitReadingsBuilderService;
		this.fieldVisitDescriptionService = fieldVisitDescriptionService;
		this.fieldVisitDataService = fieldVisitDataService;
		this.locationDescriptionListService = locationDescriptionListService;
		this.timeSeriesDataService = timeSeriesDataService;
		this.qualifierLookupService = qualifierLookupService;
	}

	@LogExecutionTime
	public SiteVisitPeakReport buildReport(SiteVisitPeakRequestParameters requestParameters, String requestingUser) {
		SiteVisitPeakReport report = new SiteVisitPeakReport();
		
		LOG.debug("Get time series descriptions.");
		TimeSeriesDescription primaryDescription = timeSeriesDescriptionListService.getTimeSeriesDescription(requestParameters.getPrimaryTimeseriesIdentifier());
		
		LOG.debug("Get zone offset.");
		ZoneOffset zoneOffset = TimeSeriesUtils.getZoneOffset(primaryDescription);

		LOG.debug("Get corrected primary time series.");
		TimeSeriesDataServiceResponse primaryTsCorrected = timeSeriesDataService.get(requestParameters.getPrimaryTimeseriesIdentifier(), requestParameters, zoneOffset, false, false, false, null);

		LOG.debug("Get field visit data.");
		report.setReadings(getFieldVisitReadings(primaryDescription.getLocationIdentifier(), zoneOffset, requestParameters, primaryTsCorrected));
				
		LOG.debug("Get time series metadata.");
		report.setReportMetadata(getMetadata(requestParameters, primaryDescription, primaryTsCorrected));
		return report;
	}

	@LogExecutionTime
	protected List<FieldVisitReading> getFieldVisitReadings(String locationIdentifier, ZoneOffset zoneOffset, SiteVisitPeakRequestParameters requestParameters, TimeSeriesDataServiceResponse primaryTsCorrected) {
		List<FieldVisitReading> readings = new ArrayList<>();

		// Process field visits
		LOG.debug("Process field visits.");
		for (FieldVisitDescription fieldVisitDescription : fieldVisitDescriptionService.getDescriptions(locationIdentifier, zoneOffset, requestParameters)) {
			FieldVisitDataServiceResponse fieldVisitDataServiceResponse = fieldVisitDataService.get(fieldVisitDescription.getIdentifier());
			List<FieldVisitReading> rawReadings = fieldVisitReadingsBuilderService.extractReadings(fieldVisitDescription.getStartTime(), fieldVisitDataServiceResponse, null, includeInspections);

			// Keep only ExtremeMax Readings
			LOG.debug("Filter ExtremeMax Readings from field visits.");
			if(rawReadings != null && !rawReadings.isEmpty()) {
				readings.addAll(rawReadings.stream()
					.filter(r -> ReadingType.ExtremeMax.equals(r.getReadingType()))
					.collect(Collectors.toList())
				);
			}
		}

		// Process Extracted Readings
		// Add last valid visits
		new LastValidVisitCalculator().fill(readings);

		// Add associated IV data
		LOG.debug("Add associated instantaneous values to ExtremeMax readings");
		for(FieldVisitReading reading : readings) {
			addAssociatedIvDataToReading(reading, primaryTsCorrected);
		}

		return readings;
	}

	@LogExecutionTime
	protected FieldVisitReading addAssociatedIvDataToReading(FieldVisitReading reading, TimeSeriesDataServiceResponse primaryTsCorrected) {
		if (null != reading.getLastVisitPrior()) {
			LOG.debug("Get time series points between field visits.");
			List<TimeSeriesPoint> points = getPointsBetweenDates(reading.getLastVisitPrior(), reading.getVisitTime(), primaryTsCorrected.getPoints());
						
			LOG.debug("Get associated qualifiers.");
			List<AssociatedIvQualifier> qualifiers = getQualifiersBetweenDates(reading.getLastVisitPrior(), reading.getVisitTime(), primaryTsCorrected.getQualifiers())
				.stream().map(q -> new AssociatedIvQualifier(q)).collect(Collectors.toList());
			
			if(qualifiers != null && !qualifiers.isEmpty()) {
				reading.setAssociatedIvQualifiers(qualifiers);
			}
			
			if(points != null && !points.isEmpty()) {
				MinMaxData minMaxData = TimeSeriesUtils.getMinMaxData(points);
				MinMaxPoint minMaxPoint = minMaxData.getMax().get(minMaxData.getMax().size()-1);
				reading.setAssociatedIvTime(minMaxPoint.getTime());
				reading.setAssociatedIvValue(minMaxPoint.getValue().toPlainString());
			}
		}

		return reading;
	}

	@LogExecutionTime
	protected List<TimeSeriesPoint> getPointsBetweenDates(Instant startDate, Instant endDate, List<TimeSeriesPoint> points) {
		if(points != null && !points.isEmpty()) {
			List<TimeSeriesPoint> filteredPoints = new ArrayList<>();
			LOG.debug("Filter time series points by start and end dates.");
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

	@LogExecutionTime
	protected List<Qualifier> getQualifiersBetweenDates(Instant startDate, Instant endDate, List<Qualifier> qualifiers) {
		if(qualifiers != null && !qualifiers.isEmpty()) {
			List<Qualifier> filteredQualifiers = new ArrayList<>();
			LOG.debug("Filter qualifiers by start and end dates of the time series points.");
			for(Qualifier qual : qualifiers) {
				if(AqcuTimeUtils.doesTimeRangeOverlap(startDate, endDate, qual.getStartTime(), qual.getEndTime())) {
					filteredQualifiers.add(qual);
				}
			}
			return filteredQualifiers;
		}

		return new ArrayList<>();
	}

	@LogExecutionTime
	protected SVPReportMetadata getMetadata(SiteVisitPeakRequestParameters requestParameters, TimeSeriesDescription primaryDescription, TimeSeriesDataServiceResponse primaryTsCorrected) {
		SVPReportMetadata metadata = new SVPReportMetadata();
		metadata.setTitle(REPORT_TITLE);
		metadata.setExcludeComments(requestParameters.getExcludeComments());
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