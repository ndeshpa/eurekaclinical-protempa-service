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
import org.protempa.PropositionDefinition;
import org.protempa.TemporalExtendedPropositionDefinition;

import java.util.ArrayList;
import java.util.List;

import static edu.emory.cci.aiw.cvrg.eureka.etl.conversion.ConversionUtil.unit;
import java.util.HashMap;
import java.util.Map;
import org.eurekaclinical.eureka.client.comm.Phenotype;
import org.eurekaclinical.eureka.client.comm.PhenotypeField;
import org.eurekaclinical.eureka.client.comm.RelatedPhenotypeField;
import org.eurekaclinical.eureka.client.comm.Sequence;
import org.eurekaclinical.eureka.client.comm.exception.PhenotypeHandlingException;
import org.protempa.proposition.interval.Interval.Side;
import org.protempa.SimpleGapFunction;
import org.protempa.TemporalPatternOffset;
import org.protempa.proposition.interval.Relation;

final class SequenceConverter extends AbstractConverter
		implements
		PropositionDefinitionConverter<Sequence, HighLevelAbstractionDefinition> {

	private PropositionDefinitionConverterVisitor converterVisitor;
	private HighLevelAbstractionDefinition primary;
	private String primaryPropId;
	private final Map<String, TemporalExtendedPropositionDefinition> extendedProps;

	public SequenceConverter() {
		this.extendedProps = new HashMap<>();
	}

	@Override
	public HighLevelAbstractionDefinition getPrimaryPropositionDefinition() {
		return primary;
	}
	
	@Override
	public String getPrimaryPropositionId() {
		return primaryPropId;
	}

	public void setConverterVisitor(PropositionDefinitionConverterVisitor inVisitor) {
		converterVisitor = inVisitor;
	}

	@Override
	public List<PropositionDefinition> convert(Sequence sequenceEntity) throws PhenotypeHandlingException {
		List<PropositionDefinition> result = new ArrayList<>();
		String propId = toPropositionId(sequenceEntity);
		this.primaryPropId = propId;
		if (this.converterVisitor.addPropositionId(propId)) {
			HighLevelAbstractionDefinition p = new HighLevelAbstractionDefinition(
					propId);
			TemporalExtendedPropositionDefinition primaryEP = 
					buildExtendedProposition(
					sequenceEntity.getPrimaryPhenotype());
			if (sequenceEntity.getRelatedPhenotypes() != null) {
				for (RelatedPhenotypeField rel : sequenceEntity.getRelatedPhenotypes()) {
					PhenotypeField lhs = rel.getPhenotypeField();
					converterVisitor.visit(lhs);
					result.addAll(
							converterVisitor.getPropositionDefinitions());
					TemporalExtendedPropositionDefinition tepdLhs = buildExtendedProposition(rel
							.getPhenotypeField());

					PhenotypeField rhs =
							rel.getSequentialPhenotypeField();
					converterVisitor.visit(rhs);
					result.addAll(converterVisitor.getPropositionDefinitions());
					TemporalExtendedPropositionDefinition tepdRhs = buildExtendedProposition(rel
							.getSequentialPhenotypeField());

					p.add(tepdLhs);
					p.add(tepdRhs);
					p.setRelation(tepdLhs, tepdRhs, buildRelation(rel));
				}
			}
			
			p.setConcatenable(false);
			p.setSolid(false);
			p.setGapFunction(new SimpleGapFunction(0, null));
			
			TemporalPatternOffset temporalOffsets = new TemporalPatternOffset();
			temporalOffsets.setStartTemporalExtendedPropositionDefinition(primaryEP);
			temporalOffsets.setStartIntervalSide(Side.START);
			temporalOffsets.setStartOffset(0);
			temporalOffsets.setStartOffsetUnits(null);
			temporalOffsets.setFinishTemporalExtendedPropositionDefinition(primaryEP);
			temporalOffsets.setFinishIntervalSide(Side.FINISH);
			temporalOffsets.setFinishOffset(0);
			temporalOffsets.setFinishOffsetUnits(null);
			p.setTemporalOffset(temporalOffsets);
			p.setDisplayName(sequenceEntity.getDisplayName());
			p.setDescription(sequenceEntity.getDescription());
			p.setSourceId(sourceId(sequenceEntity));
			this.primary = p;
			result.add(p);
		}
		
		
		return result;
	}

	private TemporalExtendedPropositionDefinition buildExtendedProposition(
			PhenotypeField ep) {
		TemporalExtendedPropositionDefinition tepd =
				this.extendedProps.get(ep.getPhenotypeKey());
		if (tepd == null) {
			tepd = 
					ConversionUtil.buildExtendedPropositionDefinition(
					ep);

			this.extendedProps.put(ep.getPhenotypeKey(), tepd);
		}

		return tepd;
	}

	private org.protempa.proposition.interval.Relation buildRelation(
			RelatedPhenotypeField rel) {
		return new org.protempa.proposition.interval.Relation(
				null, null, null, null, 
				null, 
				null,
				null, 
				null,
				rel.getRelationMinCount(), 
				unit(rel.getRelationMinUnits()),
				rel.getRelationMaxCount(), 
				unit(rel.getRelationMaxUnits()), 
				null, null, null, null);
	}
}
