/*
 * #%L
 * Eureka Services
 * %%
 * Copyright (C) 2012 - 2013 Emory University
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
package edu.emory.cci.aiw.cvrg.eureka.etl.conversion;

import static edu.emory.cci.aiw.cvrg.eureka.etl.conversion.ConversionUtil.extractContextDefinition;
import static edu.emory.cci.aiw.cvrg.eureka.etl.conversion.ConversionUtil.thresholdOperationName;
import java.util.ArrayList;
import java.util.List;
import org.eurekaclinical.eureka.client.comm.PhenotypeField;
import org.eurekaclinical.eureka.client.comm.ValueThreshold;
import org.eurekaclinical.eureka.client.comm.ValueThresholds;
import org.eurekaclinical.eureka.client.comm.exception.PhenotypeHandlingException;
import org.protempa.CompoundLowLevelAbstractionDefinition;
import org.protempa.ContextDefinition;
import org.protempa.LowLevelAbstractionDefinition;
import org.protempa.PropositionDefinition;
import org.protempa.SimpleGapFunction;
import org.protempa.SlidingWindowWidthMode;
import org.protempa.ValueClassification;

public final class ValueThresholdsCompoundLowLevelAbstractionConverter
		extends AbstractValueThresholdGroupEntityConverter
		implements
		PropositionDefinitionConverter<ValueThresholds, CompoundLowLevelAbstractionDefinition> {

	private PropositionDefinitionConverterVisitor converterVisitor;
	private CompoundLowLevelAbstractionDefinition primary;
	private String primaryPropId;

	public void setConverterVisitor(PropositionDefinitionConverterVisitor inConverterVisitor) {
		converterVisitor = inConverterVisitor;
	}

	@Override
	public CompoundLowLevelAbstractionDefinition getPrimaryPropositionDefinition() {
		return primary;
	}

	@Override
	public String getPrimaryPropositionId() {
		return primaryPropId;
	}

	@Override
	public List<PropositionDefinition> convert(
			ValueThresholds entity) throws PhenotypeHandlingException {
		List<PropositionDefinition> result = new ArrayList<>();
		String propId = toPropositionId(entity);
		if (this.converterVisitor.addPropositionId(propId)) {
			CompoundLowLevelAbstractionDefinition wrapped = new CompoundLowLevelAbstractionDefinition(
					propId);
			wrapped.setDisplayName(entity.getDisplayName());
			wrapped.setDescription(entity.getDescription());

			if (thresholdOperationName(entity.getThresholdsOperator()).equalsIgnoreCase("any")) {
				wrapped.setValueDefinitionMatchOperator(CompoundLowLevelAbstractionDefinition.ValueDefinitionMatchOperator.ANY);
			} else if (thresholdOperationName(entity.getThresholdsOperator())
					.equalsIgnoreCase("all")) {
				wrapped.setValueDefinitionMatchOperator(CompoundLowLevelAbstractionDefinition.ValueDefinitionMatchOperator.ALL);
			} else {
				throw new IllegalStateException("valueDefinitionMatchOperator"
						+ " can only be ANY or ALL");
			}

			wrapped.setGapFunction(new SimpleGapFunction(0, null));

			List<LowLevelAbstractionDefinition> intermediates = new ArrayList<>();
			for (int idx=0; idx< entity.getValueThresholds().size(); idx++) {
                            ValueThreshold v = entity.getValueThresholds().get(idx);
				this.converterVisitor.visit(v.getPhenotype());
				result.addAll(this.converterVisitor.getPropositionDefinitions());
				LowLevelAbstractionDefinition def =
						new LowLevelAbstractionDefinition(
						entity.getKey() + "_SUB" + idx);
				def.setConcatenable(false);
				def.addPrimitiveParameterId(this.converterVisitor.getPrimaryPropositionId());
				def.setMinimumNumberOfValues(1);
				def.setMaximumNumberOfValues(1);
				def.setAlgorithmId("stateDetector");
				def.setGapFunction(new SimpleGapFunction(Integer.valueOf(0), null));
				thresholdToValueDefinitions(entity, v, def);
				def.setSlidingWindowWidthMode(SlidingWindowWidthMode.DEFAULT);
				def.setGapFunction(new SimpleGapFunction(0, null));
				List<PhenotypeField> extendedPhenotypes =
						v.getRelatedPhenotypes();
				if (extendedPhenotypes != null && !extendedPhenotypes.isEmpty()) {
					ContextDefinition cd = 
							extractContextDefinition(entity, 
							extendedPhenotypes, v);
					result.add(cd);
					def.setContextId(cd.getId());
				}
				def.setSourceId(sourceId(entity));
				intermediates.add(def);
			}
			result.addAll(intermediates);

			for (LowLevelAbstractionDefinition def : intermediates) {
				wrapped.addValueClassification(new ValueClassification(asValueString(entity),
						def.getId(), asValueString(entity)));
				wrapped.addValueClassification(new ValueClassification(asValueCompString(entity),
						def.getId(), asValueCompString(entity)));
			}

			wrapped.setSourceId(sourceId(entity));
			result.add(wrapped);
			this.primary = wrapped;
			this.primaryPropId = wrapped.getPropositionId();
		}

		return result;
	}
}
