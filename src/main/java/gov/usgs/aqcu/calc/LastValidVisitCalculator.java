package gov.usgs.aqcu.calc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

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

			// Handle null readings
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

			// Monitoring Method
			String r1MonitoringMethod = MethodCategory.getMethodCategory(reading1.getMonitoringMethod());
			String r2MonitoringMethod = MethodCategory.getMethodCategory(reading2.getMonitoringMethod());

			if(sortOrder == 0 && r1MonitoringMethod != null && r2MonitoringMethod != null) {
				sortOrder = r1MonitoringMethod.compareTo(r2MonitoringMethod);
			}

			if(sortOrder == 0 && reading1.getValue() != null && reading2.getValue() != null) {
				sortOrder = reading1.getValue().compareTo(reading2.getValue());
			}

			return sortOrder;
		}
	};

	protected enum MethodCategory {
		CRESTSTAGE("crest stage"),
		MAXMIN("max-min indicator"),
		HIGHWATERMARK("high water mark");

		private String methodIdentifier;
		MethodCategory(String methodIdentifier) {
			this.methodIdentifier = methodIdentifier;
		}
		public String getMethodIdentifier() {
			return methodIdentifier;
		}

		public static String getMethodCategory(String method) {
			for(MethodCategory category : MethodCategory.values()) {
				if (method.toLowerCase().contains(category.getMethodIdentifier())) {
					return category.getMethodIdentifier();
				}
			}
			return null;
		}
	};



	public List<FieldVisitReading> fill(List<Pair<String, FieldVisitReading>> pairs) {
		Collections.sort(pairs, VISIT_READING_COMPARATOR);
		// lastVisitMap maps a method string to the last time that method was visited
		Map<String, Instant> lastVisitMap = new HashMap<>();

		List<FieldVisitReading> filledReadings = new ArrayList<>();

		for(int i = 0; i < pairs.size(); i++) {
			Pair<String, FieldVisitReading> pair = pairs.get(i);
			FieldVisitReading reading = pair.getRight();
			
			if(isValidReading(reading)) {
				// Need to set the lastVisitTime based on the last of this type of method.
				String monitoringMethod = reading.getMonitoringMethod();
				String methodCategory = MethodCategory.getMethodCategory(monitoringMethod);
				Instant lastCategoryVisit = lastVisitMap.get(methodCategory);
				System.out.println(methodCategory);
				System.out.println(lastCategoryVisit);
				if (lastCategoryVisit != null) {
					reading.setLastVisitPrior(lastCategoryVisit);
				}

				curVisitTime = reading.getVisitTime();
				lastVisitMap.put(methodCategory, curVisitTime);
				curVisitIdentifier = pair.getLeft();
			}

			filledReadings.add(reading);
		}
		
		return filledReadings;
	}

	private boolean isValidReading(FieldVisitReading reading) {
		String value = reading.getValue();
		return value != null && (value.matches("[-+]?\\d*\\.?\\d+") || "no mark".equals(value.toLowerCase().trim()) || "over topped".equals(value.toLowerCase().trim())) && reading.getMonitoringMethod() != null;
	}
}
