package gov.usgs.aqcu.calc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import gov.usgs.aqcu.model.FieldVisitReading;

public class LastValidVisitCalculator {
	private String curVisitIdentifier = null;
	private Instant curVisitTime = null;
	private Instant lastVisitTime = null;

	protected final Comparator<Pair<String, FieldVisitReading>> VISIT_READING_COMPARATOR = new Comparator<Pair<String, FieldVisitReading>>() {
		@Override
		public int compare(Pair<String, FieldVisitReading> pair1, Pair<String, FieldVisitReading> pair2) {
			FieldVisitReading reading1 = pair1.getRight();
			FieldVisitReading reading2 = pair2.getRight();

			if(reading1 == null && reading2 == null) {
				return 0;
			}
			
			if(reading1 == null && reading2 != null) {
				return -1;
			}
			
			if(reading1 != null && reading2 == null) {
				return 1;
			}
			
			int sortOrder = reading1.getVisitTime().compareTo(reading2.getVisitTime());

			if(sortOrder == 0 && reading1.getTime() != null && reading2.getTime() != null) {
				sortOrder = reading1.getTime().compareTo(reading2.getTime());
			}

			return sortOrder;
		}
	};

	public List<FieldVisitReading> fill(List<Pair<String, FieldVisitReading>> pairs) {
		Collections.sort(pairs, VISIT_READING_COMPARATOR);

		List<FieldVisitReading> filledReadings = new ArrayList<>();

		for(int i = 0; i < pairs.size(); i++) {
			Pair<String, FieldVisitReading> pair = pairs.get(i);
			FieldVisitReading reading = pair.getRight();
			
			if(isValidReading(reading)) {
				if(curVisitIdentifier != pair.getLeft()) {
					lastVisitTime = curVisitTime;
					curVisitIdentifier = pair.getLeft();
					curVisitTime = pair.getRight().getVisitTime();
				}

				reading.setLastVisitPrior(lastVisitTime);
			}
				
			filledReadings.add(reading);
		}
		
		return filledReadings;
	}

	private boolean isValidReading(FieldVisitReading reading) {
		String value = reading.getValue();
		return value != null && (value.matches("[-+]?\\d*\\.?\\d+") || "no mark".equals(value.toLowerCase().trim())) && reading.getMonitoringMethod() != null;
	}
}
