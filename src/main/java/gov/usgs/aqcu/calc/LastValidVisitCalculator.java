package gov.usgs.aqcu.calc;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gov.usgs.aqcu.model.FieldVisitReading;

//TODO needs testing
public class LastValidVisitCalculator {
	Instant lastVisit = null;

	public List<FieldVisitReading> fill(List<FieldVisitReading> readings) {
		Collections.sort(readings, new Comparator<FieldVisitReading>() {
			@Override
			public int compare(FieldVisitReading reading1, FieldVisitReading reading2) {
				if(reading1 == null && reading2 == null) {
					return 0;
				}
				
				if(reading1 == null && reading2 != null) {
					return -1;
				}
				
				if(reading1 != null && reading2 == null) {
					return 1;
				}
				
				return reading1.getVisitTime().compareTo(reading2.getVisitTime());
			}
		});
		
		for(FieldVisitReading reading : readings) {
			String value = reading.getValue();
			
			if(value != null && (value.matches("[-+]?\\d*\\.?\\d+") || "no mark".equals(value.toLowerCase().trim())) && reading.getMonitoringMethod() != null) {
				if(lastVisit != null) {
					reading.setLastVisitPrior(lastVisit);
				}
				
				lastVisit = reading.getVisitTime();
			}
		}
		
		return readings;
	}
}
