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

import org.protempa.HighLevelAbstractionDefinition;
import org.protempa.MinMaxGapFunction;
import org.protempa.PropositionDefinition;
import org.protempa.TemporalExtendedParameterDefinition;
import org.protempa.proposition.interval.Relation;

import java.util.ArrayList;
import java.util.List;

import static edu.emory.cci.aiw.cvrg.eureka.etl.conversion.ConversionUtil.unit;
import static edu.emory.cci.aiw.cvrg.eureka.etl.conversion.ConversionUtil.frequencyTypeName;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eurekaclinical.eureka.client.comm.Frequency;
import org.eurekaclinical.eureka.client.comm.Phenotype;
import org.eurekaclinical.eureka.client.comm.exception.PhenotypeHandlingException;
import org.protempa.CompoundLowLevelAbstractionDefinition;
import org.protempa.SimpleGapFunction;
import org.protempa.SliceDefinition;
import org.protempa.TemporalExtendedPropositionDefinition;
import org.protempa.ValueClassification;

public final class FrequencyValueThresholdConverter extends AbstractConverter 
		implements FrequencyConverter {

	private PropositionDefinitionConverterVisitor converterVisitor;
	private HighLevelAbstractionDefinition primary;
	private String primaryPropId;

	@Override
	public HighLevelAbstractionDefinition getPrimaryPropositionDefinition() {
		return primary;
	}

	@Override
	public String getPrimaryPropositionId() {
		return primaryPropId;
	}

	public void setConverterVisitor(
			PropositionDefinitionConverterVisitor inVisitor) {
		converterVisitor = inVisitor;
	}

	@Override
	public List<PropositionDefinition> convert(Frequency entity) {
		List<PropositionDefinition> result =
				new ArrayList<>();

		String propId = toPropositionId(entity);
		this.primaryPropId = propId;

		if (this.converterVisitor.addPropositionId(propId)) {
			Phenotype abstractedFrom = entity;
					
                    try {
                        abstractedFrom.accept(this.converterVisitor);
                    } catch (PhenotypeHandlingException ex) {
                        Logger.getLogger(FrequencyValueThresholdConverter.class.getName()).log(Level.SEVERE, null, ex);
                    }
			Collection<PropositionDefinition> intermediates =
					this.converterVisitor.getPropositionDefinitions();
			result.addAll(intermediates);
			String wrapperPropId = entity.getKey() + "_SUB";
                        
			if (entity.getIsConsecutive()) {
				String abstractedFromPrimaryPropId =
					this.converterVisitor.getPrimaryPropositionId();
				CompoundLowLevelAbstractionDefinition frequencyWrapper =
						new CompoundLowLevelAbstractionDefinition(
						wrapperPropId);
				frequencyWrapper.setMinimumNumberOfValues(
						entity.getAtLeast());
				frequencyWrapper.setGapFunction(
						new SimpleGapFunction(Integer.valueOf(0), null));
				ValueClassification vc = new ValueClassification(
						asValueString(entity),
						abstractedFromPrimaryPropId,
						asValueString(abstractedFrom));
				frequencyWrapper.addValueClassification(vc);
				ValueClassification vcComp = new ValueClassification(
						asValueCompString(entity),
						abstractedFromPrimaryPropId,
						asValueCompString(abstractedFrom));
				frequencyWrapper.addValueClassification(vcComp);
				frequencyWrapper.setGapFunctionBetweenValues(
						new MinMaxGapFunction(entity.getWithinAtLeast(),
						unit(entity.getWithinAtLeastUnits()),
						entity.getWithinAtMost(),
						unit(entity.getWithinAtMostUnits())));
				frequencyWrapper.setSourceId(sourceId(entity));
				result.add(frequencyWrapper);
				
				TemporalExtendedPropositionDefinition tepdOuter;
				if (frequencyTypeName(entity.getFrequencyType()).equals("at least")) {
					TemporalExtendedParameterDefinition tepd =
							new TemporalExtendedParameterDefinition(wrapperPropId);
					tepd.setValue(asValue(entity));
					tepdOuter = tepd;
				} else if (frequencyTypeName(entity.getFrequencyType()).equals("first")) {
					frequencyWrapper.setSkip(1);
					String subWrapperPropId = wrapperPropId + "SUB";
					SliceDefinition sp = new SliceDefinition(subWrapperPropId);
					sp.setMinIndex(0);
					sp.setMaxIndex(1);
					TemporalExtendedParameterDefinition tepd2 =
							new TemporalExtendedParameterDefinition(wrapperPropId);
					tepd2.setValue(asValue(entity));
					sp.add(tepd2);
					sp.setSourceId(sourceId(entity));
					result.add(sp);

					TemporalExtendedPropositionDefinition tepd =
							new TemporalExtendedPropositionDefinition(subWrapperPropId);

					tepdOuter = tepd;
				} else {
					throw new IllegalStateException("invalid frequency type: " + frequencyTypeName(entity.getFrequencyType()));
				}
				HighLevelAbstractionDefinition hlad =
						new HighLevelAbstractionDefinition(propId);
				hlad.add(tepdOuter);
				hlad.setRelation(tepdOuter, tepdOuter, new Relation());

				hlad.setDisplayName(entity.getDisplayName());
				hlad.setDescription(entity.getDescription());
				hlad.setSourceId(sourceId(entity));
//				hlad.setGapFunction(
//						new SimpleGapFunction(Integer.valueOf(0), null));
				result.add(hlad);
				this.primary = hlad;
			} else {
				HighLevelAbstractionDefinition p =
						new HighLevelAbstractionDefinition(propId);
				p.setDisplayName(entity.getDisplayName());
				p.setDescription(entity.getDescription());
				if (frequencyTypeName(entity.getFrequencyType()).equals("at least")) {
					TemporalExtendedPropositionDefinition[] tepds =
							new TemporalExtendedPropositionDefinition[entity.getAtLeast()];
					for (int i = 0; i < entity.getAtLeast(); i++) {
						TemporalExtendedPropositionDefinition tepd =
								ConversionUtil.buildExtendedPropositionDefinition(
								entity.getPhenotype());
						tepds[i] = tepd;
						p.add(tepd);
					}
					if (tepds.length > 1) {
						for (int i = 0; i < tepds.length - 1; i++) {
							Relation rel = new Relation(null, null, null, null, null,
									null, null, null, entity.getWithinAtLeast(),
									unit(entity.getWithinAtLeastUnits()),
									entity.getWithinAtMost(),
									unit(entity.getWithinAtMostUnits()), null, null,
									null, null);
							p.setRelation(tepds[i], tepds[i + 1], rel);
						}
					} else {
						p.setRelation(tepds[0], tepds[0], new Relation());
					}
				} else if (frequencyTypeName(entity.getFrequencyType()).equals("first")) {
					SliceDefinition sp = new SliceDefinition(wrapperPropId);
					sp.setDisplayName(entity.getDisplayName());
					sp.setDescription(entity.getDescription());
					sp.setMergedInterval(true);
					sp.setGapFunction(new SimpleGapFunction(0, null));
					TemporalExtendedPropositionDefinition tepd =
							ConversionUtil.buildExtendedPropositionDefinition(
							entity.getPhenotype());
					sp.add(tepd);
					sp.setMinIndex(0);
					sp.setMaxIndex(entity.getAtLeast());
					sp.setSourceId(sourceId(entity));
					result.add(sp);
					TemporalExtendedPropositionDefinition tepdForSlice =
							new TemporalExtendedPropositionDefinition(wrapperPropId);
					p.add(tepdForSlice);
					p.setRelation(tepdForSlice, tepdForSlice, new Relation());
				} else {
					throw new IllegalStateException("invalid frequency type: " + frequencyTypeName(entity.getFrequencyType()));
				}
				p.setGapFunction(new SimpleGapFunction(0, null));
				p.setSourceId(sourceId(entity));
				this.primary = p;
				result.add(p);
			}
			this.primaryPropId = this.primary.getPropositionId();
		}


		return result;
	}
	
}
