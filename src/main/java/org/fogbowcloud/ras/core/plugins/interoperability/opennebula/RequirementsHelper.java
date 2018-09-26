package org.fogbowcloud.ras.core.plugins.interoperability.opennebula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.fogbowcloud.ras.core.models.HardwareRequirements;

import condor.classad.AttrRef;
import condor.classad.ClassAdParser;
import condor.classad.Env;
import condor.classad.Expr;
import condor.classad.Op;
import condor.classad.RecordExpr;

public class RequirementsHelper {
	
	private static final String GLUE_LOCATION_TERM = "Glue2CloudComputeManagerID";
	public static final String GLUE_DISK_TERM = "Glue2Disk";
	public static final String GLUE_MEM_RAM_TERM = "Glue2RAM";
	public static final String GLUE_VCPU_TERM = "Glue2vCPU";
	public static final String VALUE_IGNORED = "-1";
	private static final String VALUE_ZERO = "0";
	
	public static boolean checkSyntax(String requirementsString) {
		if (requirementsString == null || requirementsString.isEmpty()) {
			return true;
		}
		try {
			ClassAdParser adParser = new ClassAdParser(requirementsString);
			if (adParser.parse() != null) {
				return true;
			}
		} catch (Exception e) {}
		return false;
	}

	public static String getSmallestValueForAttribute(String requirementsStr, String attrName) {
		if (requirementsStr == null) {
			return VALUE_ZERO;
		}
		ClassAdParser classAdParser = new ClassAdParser(requirementsStr);
		Expr parsedClassAd = classAdParser.parse();
		if (!(parsedClassAd instanceof Op)) {
			return VALUE_ZERO;
		}
		Op expr = (Op) parsedClassAd;		
		Expr variableExpression = extractVariableExpression(expr, attrName);
		if (variableExpression == null || !(variableExpression instanceof Op)) {
			return VALUE_ZERO;
		}
		Op opForAtt = (Op) variableExpression;
		
		List<Integer> values = new ArrayList<Integer>();
		List<ValueAndOperator> findValuesInRequiremets = findValuesInRequiremets(expr, attrName);
		for (ValueAndOperator valueAndOperator : findValuesInRequiremets) {
			int value = Integer.parseInt(valueAndOperator.getValue());
			if (checkValue(opForAtt, attrName, String.valueOf(value - 1))) {
				values.add(value - 1);
			} else if (checkValue(opForAtt, attrName, String.valueOf(value))) {
				values.add(value);
			} else if (checkValue(opForAtt, attrName, String.valueOf(value + 1))) {
				values.add(value + 1);
			}
		}
		
		Collections.sort(values);
		
		if (values.size() > 0) {
			return String.valueOf(values.get(0));			
		}
		return VALUE_ZERO;
	}
	
	private static boolean checkValue(Op op, String attrName, String value) {
		Env env = new Env();
		env.push((RecordExpr) new ClassAdParser("[" + attrName + " = " + value + "]").parse());
		return op.eval(env).isTrue();		
	}
	
	public static boolean matches(HardwareRequirements flavor, String requirementsStr) {
		try {
			if (requirementsStr == null  || requirementsStr.isEmpty()) {
				return true;
			}
			
			ClassAdParser classAdParser = new ClassAdParser(requirementsStr);		
			Op expr = (Op) classAdParser.parse();
			
			List<String> listAttrSearched = new ArrayList<String>();
			List<String> listAttrProvided = new ArrayList<String>();
			listAttrProvided.add(GLUE_DISK_TERM);
			listAttrProvided.add(GLUE_MEM_RAM_TERM);
			listAttrProvided.add(GLUE_VCPU_TERM);
			
			Env env = new Env();
			String value = null;
			for (String attr : listAttrProvided) {
				List<ValueAndOperator> findValuesInRequiremets = findValuesInRequiremets(expr, attr);
				if (findValuesInRequiremets.size() > 0) {
					listAttrSearched.add(attr);
					if (attr.equals(GLUE_DISK_TERM) 
							&& requirementsStr.contains(GLUE_DISK_TERM)) {
						value = String.valueOf(flavor.getDisk());
						if (value == null || 
								(value != null && value.equals(VALUE_IGNORED) || value.isEmpty())) {
							listAttrSearched.remove(attr);							
						}
					} else if (attr.equals(GLUE_MEM_RAM_TERM) 
							&& requirementsStr.contains(GLUE_MEM_RAM_TERM)) {
						value = String.valueOf(flavor.getRam());
						if (value == null || 
								value != null && (value.equals(VALUE_IGNORED) || value.isEmpty())) {
							listAttrSearched.remove(attr);							
						}
					} else if (attr.equals(GLUE_VCPU_TERM) 
							&& requirementsStr.contains(GLUE_VCPU_TERM)) {
						value = String.valueOf(flavor.getCpu());
						if (value == null || 
								value != null && (value.equals(VALUE_IGNORED) || value.isEmpty())) {
							listAttrSearched.remove(attr);
						}
					}
					env.push((RecordExpr) new ClassAdParser("[" + attr + " = " + value + "]").parse());
				}
			}					
			
			if (listAttrSearched.isEmpty()) {
				return true;
			}
			expr = extractVariablesExpression(expr, listAttrSearched);
			
			return expr.eval(env).isTrue();
		} catch (Exception e) {
			return true;
		}
	}
	
	public static HardwareRequirements findSmallestFlavor(List<HardwareRequirements> flavors, String requirementsStr) {
		List<HardwareRequirements> listFlavor = new ArrayList<>();
		for (HardwareRequirements flavor : flavors) {
			if (matches(flavor, requirementsStr)) {
				listFlavor.add(flavor);
			}
		}

		if (listFlavor.isEmpty()) {
			return null;
		}

		Collections.sort(listFlavor, new FlavorComparator());

		return listFlavor.get(0);
	}	
	
	public static Op extractVariableExpression(Op expr, String attName) {
		if (expr.arg1 instanceof AttrRef) {
			AttrRef attr = (AttrRef) expr.arg1;
			if (!attr.name.rawString().equals(attName)) {
				return null;
			}
			return expr;
		}
		Expr left = expr.arg1;
		if (left instanceof Op) {
			left = extractVariableExpression((Op) expr.arg1, attName);
		}
		Expr right = expr.arg2;
		if (right instanceof Op) {
			right = extractVariableExpression((Op) expr.arg2, attName);
		}
		try {
			if (left == null) {
				return (Op) right;
			} else if (right == null) {
				return (Op) left;
			}			
		} catch (Exception e) {	
			return null;
		}
		return new Op(expr.op, left, right);
	}
	
	public static Op extractVariablesExpression(Op expr, List<String> listAttName) {
		if (expr.arg1 instanceof AttrRef) {
			AttrRef attr = (AttrRef) expr.arg1;
			boolean thereIs = false;
			for (String attName : listAttName) {
				if (attr.name.rawString().equals(attName)) {
					thereIs = true;
				}
			}
			if (thereIs) {
				return expr;				
			}
			return null;
		}
		Expr left = expr.arg1;
		if (left instanceof Op) {
			left = extractVariablesExpression((Op) expr.arg1, listAttName);
		}
		Expr right = expr.arg2;
		if (right instanceof Op) {
			right = extractVariablesExpression((Op) expr.arg2, listAttName);
		}
		try {
			if (left == null) {
				return (Op) right;
			} else if (right == null) {
				return (Op) left;
			}			
		} catch (Exception e) {
			return null;
		}
		return new Op(expr.op, left, right);
	}

	protected static String quoteLocation(String location) {
		if (location == null) {
			return null;
		}
		if (!location.startsWith("\"")) {
			location = "\"" + location;
		}
		if (!location.endsWith("\"")) {
			location = location + "\"";
		}
		return location;
	}
	
	public static boolean matchLocation(String requirementsStr, String valueLocation) {
		if (!hasLocation(requirementsStr)) {
			return true;
		}
		
		ClassAdParser classAdParser = new ClassAdParser(requirementsStr);
		Op expr = (Op) classAdParser.parse();

		valueLocation = quoteLocation(valueLocation);
		Env env = new Env();
		env.push((RecordExpr) new ClassAdParser("[" + GLUE_LOCATION_TERM + " = " + valueLocation
				+ "]").parse());

		Expr opForAtt = extractVariableExpression(expr, GLUE_LOCATION_TERM);
		
		if (opForAtt == null) {
			return false;
		}
		return opForAtt.eval(env).isTrue(); 
	}

	public static List<ValueAndOperator> findValuesInRequiremets(Op expr, String attName) {
		List<ValueAndOperator> valuesAndOperator = new ArrayList<ValueAndOperator>();
		if (expr.arg1 instanceof AttrRef) {
			AttrRef attr = (AttrRef) expr.arg1;
			if (attr.name.rawString().equals(attName)) {
				valuesAndOperator.add(new ValueAndOperator(expr.arg2.toString(), expr.op));
			}
			return valuesAndOperator;
		}
		if (expr.arg1 instanceof Op) {
			List<ValueAndOperator> findValuesInRequiremets = findValuesInRequiremets(
					(Op) expr.arg1, attName);
			if (findValuesInRequiremets != null) {
				valuesAndOperator.addAll(findValuesInRequiremets);
			}
		}
		if (expr.arg2 instanceof Op) {
			List<ValueAndOperator> findValuesInRequiremets = findValuesInRequiremets(
					(Op) expr.arg2, attName);
			if (findValuesInRequiremets != null) {
				valuesAndOperator.addAll(findValuesInRequiremets);
			}
		}
		return valuesAndOperator;
	}

	public static List<String> getLocations(String requirementsStr) {
		List<String> locations = new ArrayList<String>();
		if (requirementsStr == null || requirementsStr.isEmpty()) {
			return locations;
		}
		ClassAdParser classAdParser = new ClassAdParser(requirementsStr);
		Expr parsedClassAd = classAdParser.parse();
		if (!(parsedClassAd instanceof Op)) {
			return locations;
		}
		Op expr = (Op) parsedClassAd;
		List<ValueAndOperator> findValuesInRequiremets = findValuesInRequiremets(expr,
				GLUE_LOCATION_TERM);
		for (ValueAndOperator valueAndOperator : findValuesInRequiremets) {
			if (valueAndOperator.getOperator() == RecordExpr.EQUAL) {
				locations.add(valueAndOperator.getValue());
			}
		}
		return locations;
	}
	
	public static boolean hasLocation(String requirementsStr) {
		List<String> locationsInRequiremets = getLocations(requirementsStr);
		if (locationsInRequiremets.isEmpty()) {
			return false;
		}
		return true;
	}

	protected static class ValueAndOperator {
		private String value;
		private int operator;

		public ValueAndOperator(String value, int operator) {
			this.value = value;
			this.operator = operator;
		}

		public int getOperator() {
			return operator;
		}

		public String getValue() {
			return value;
		}
	}

	protected static class FlavorComparator implements Comparator<HardwareRequirements> {
		private final int MEM_VALUE_RELEVANCE = 1;
		private final int VCPU_VALUE_RELEVANCE = 1;

		@Override
		public int compare(HardwareRequirements flavorOne, HardwareRequirements flavorTwo) {
			try {
				Double oneRelevance = calculateRelevance(flavorOne, flavorTwo);
				Double twoRelevance = calculateRelevance(flavorTwo, flavorOne);
				if (oneRelevance.doubleValue() != twoRelevance.doubleValue()) {
					return oneRelevance.compareTo(twoRelevance);
				}	
				Double oneDisk = (double) flavorOne.getDisk();
				Double twoDisk = (double) flavorTwo.getDisk();
				return oneDisk.compareTo(twoDisk);
			} catch (Exception e) {
				return 0;
			}
		}

		public double calculateRelevance(HardwareRequirements flavorOne, HardwareRequirements flavorTwo) {
			double cpuOne = flavorOne.getCpu();
			double cpuTwo = flavorTwo.getCpu();
			double memOne = flavorOne.getRam();
			double memTwo = flavorTwo.getRam();

			return ((cpuOne / cpuTwo) * 1 / VCPU_VALUE_RELEVANCE)
					+ ((memOne / memTwo) * 1 / MEM_VALUE_RELEVANCE);
		}
	}
}
