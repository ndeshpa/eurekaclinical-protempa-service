package edu.emory.cci.aiw.cvrg.eureka.etl.conversion;

/*
 * #%L
 * Eureka Services
 * %%
 * Copyright (C) 2012 - 2014 Emory University
 * %%
 * This program is dual licensed under the Apache 2 and GPLv3 licenses.
 * 
 * Apache License, Version 2.0:
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * GNU General Public License version 3:
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import static edu.emory.cci.aiw.cvrg.eureka.etl.conversion.ConversionUtil.valueComparatorComplement;
import static edu.emory.cci.aiw.cvrg.eureka.etl.conversion.ConversionUtil.valueComparatorName;
import java.util.HashMap;
import java.util.Map;
import org.eurekaclinical.eureka.client.comm.ValueThreshold;
import org.eurekaclinical.eureka.client.comm.ValueThresholds;
import org.protempa.HighLevelAbstractionDefinition;
import org.protempa.LowLevelAbstractionDefinition;
import org.protempa.LowLevelAbstractionValueDefinition;
import org.protempa.SimpleGapFunction;
import org.protempa.TemporalExtendedParameterDefinition;
import org.protempa.proposition.interval.Relation;
import org.protempa.proposition.value.NominalValue;
import org.protempa.proposition.value.ValueComparator;
import org.protempa.proposition.value.ValueType;

/**
 *
 * @author Andrew Post
 */
public class AbstractValueThresholdGroupEntityConverter extends AbstractConverter {
	
	private static final Map<String, ValueComparator> VC_MAP =
			new HashMap<>();
	static {
		VC_MAP.put(">", ValueComparator.GREATER_THAN);
		VC_MAP.put(">=", ValueComparator.GREATER_THAN_OR_EQUAL_TO);
		VC_MAP.put("=", ValueComparator.EQUAL_TO);
		VC_MAP.put("not=", ValueComparator.NOT_EQUAL_TO);
		VC_MAP.put("<=", ValueComparator.LESS_THAN_OR_EQUAL_TO);
		VC_MAP.put("<", ValueComparator.LESS_THAN);
	}

	AbstractValueThresholdGroupEntityConverter() {
	}
	
	HighLevelAbstractionDefinition wrap(ValueThresholds valueThresholdGroup) {
		HighLevelAbstractionDefinition wrapper = 
				new HighLevelAbstractionDefinition(
						toPropositionId(valueThresholdGroup));
		wrapper.setDisplayName(valueThresholdGroup.getDisplayName());
		wrapper.setDescription(valueThresholdGroup.getDescription());
		TemporalExtendedParameterDefinition tepd = 
				new TemporalExtendedParameterDefinition(
				toPropositionIdWrapped(valueThresholdGroup));
		
		tepd.setValue(asValue(valueThresholdGroup));
		wrapper.add(tepd);
		Relation relation = new Relation();
		wrapper.setRelation(tepd, tepd, relation);
		wrapper.setConcatenable(false);
		wrapper.setGapFunction(new SimpleGapFunction(0, null));
		wrapper.setSolid(false);
		wrapper.setSourceId(sourceId(valueThresholdGroup));
		return wrapper;
	}
	
	void thresholdToValueDefinitions(ValueThresholds entity,
			ValueThreshold threshold,
			LowLevelAbstractionDefinition def) {
		LowLevelAbstractionValueDefinition valueDef =
				new LowLevelAbstractionValueDefinition(
				def, asValueString(entity));
		valueDef.setValue(NominalValue.getInstance(asValueString(entity)));
		if (threshold.getLowerValue() != null
				&& threshold.getLowerComp() != null) {
			valueDef.setParameterValue("minThreshold", ValueType.VALUE
					.parse(threshold.getLowerValue()));
			valueDef.setParameterComp("minThreshold", 
					VC_MAP.get(valueComparatorName(threshold.getLowerComp())));
		}
		if (threshold.getUpperValue() != null
				&& threshold.getUpperComp() != null) {
			valueDef.setParameterValue("maxThreshold", ValueType.VALUE
					.parse(threshold.getUpperValue()));
			valueDef.setParameterComp("maxThreshold", 
					VC_MAP.get(valueComparatorName(threshold.getUpperComp())));
		}
		if (threshold.getLowerValue() != null
				&& threshold.getLowerComp() != null
				&& threshold.getUpperValue() != null
				&& threshold.getUpperComp() != null) {
			LowLevelAbstractionValueDefinition comp1ValueDef =
				new LowLevelAbstractionValueDefinition(def, asValueCompString(entity));
			comp1ValueDef.setValue(NominalValue.getInstance(asValueCompString(entity)));
			comp1ValueDef.setParameterValue("maxThreshold", ValueType.VALUE
					.parse(threshold.getLowerValue()));
			comp1ValueDef.setParameterComp("maxThreshold",
					VC_MAP.get(valueComparatorComplement(valueComparatorName(threshold.getLowerComp()))));
			
			LowLevelAbstractionValueDefinition comp2ValueDef =
				new LowLevelAbstractionValueDefinition(def, asValueCompString(entity));
			comp2ValueDef.setValue(NominalValue.getInstance(asValueCompString(entity)));
			comp2ValueDef.setParameterValue("minThreshold", ValueType.VALUE
					.parse(threshold.getUpperValue()));
			comp2ValueDef.setParameterComp("minThreshold",
					VC_MAP.get(valueComparatorComplement(valueComparatorName(
					threshold.getUpperComp()))));
		} else if (threshold.getLowerValue() != null
				&& threshold.getLowerComp() != null) {
			LowLevelAbstractionValueDefinition compValueDef =
				new LowLevelAbstractionValueDefinition(def, asValueCompString(entity));
			compValueDef.setValue(NominalValue.getInstance(asValueCompString(entity)));
			compValueDef.setParameterValue("maxThreshold", ValueType.VALUE
					.parse(threshold.getLowerValue()));
			compValueDef.setParameterComp("maxThreshold",
					VC_MAP.get(valueComparatorComplement(valueComparatorName(threshold.getLowerComp()))));
		} else if (threshold.getUpperValue() != null
				&& threshold.getUpperComp() != null) {
			LowLevelAbstractionValueDefinition compValueDef =
				new LowLevelAbstractionValueDefinition(def, asValueCompString(entity));
			compValueDef.setValue(NominalValue.getInstance(asValueCompString(entity)));
			compValueDef.setParameterValue("minThreshold", ValueType.VALUE
					.parse(threshold.getUpperValue()));
			compValueDef.setParameterComp("minThreshold",
					VC_MAP.get(
					valueComparatorComplement(valueComparatorName(
					threshold.getUpperComp()))));
		}
	}
	
}
