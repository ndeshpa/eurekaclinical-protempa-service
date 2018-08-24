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

//import org.eurekaclinical.phenotype.service.entity.PhenotypeEntity;
//import org.eurekaclinical.phenotype.service.entity.ExtendedPhenotype;
//import org.eurekaclinical.phenotype.service.entity.RelationOperator;
//import org.eurekaclinical.phenotype.service.entity.TimeUnit;
//import org.eurekaclinical.phenotype.service.entity.ValueThresholdEntity;
//import org.eurekaclinical.phenotype.service.entity.ValueThresholdGroupEntity;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eurekaclinical.common.comm.clients.ClientException;
import org.eurekaclinical.eureka.client.comm.Phenotype;
import org.eurekaclinical.eureka.client.comm.PhenotypeField;
import org.eurekaclinical.eureka.client.comm.ValueThreshold;
import org.eurekaclinical.eureka.client.comm.ValueThresholds;
import org.eurekaclinical.phenotype.client.EurekaClinicalPhenotypeClient;
import org.eurekaclinical.phenotype.client.comm.FrequencyType;
import org.eurekaclinical.phenotype.client.comm.RelationOperator;
import org.eurekaclinical.phenotype.client.comm.ThresholdsOperator;
import org.eurekaclinical.phenotype.client.comm.TimeUnit;
import org.eurekaclinical.phenotype.client.comm.ValueComparator;
import org.protempa.ContextDefinition;
import org.protempa.ContextOffset;
import org.protempa.proposition.interval.Interval.Side;
import org.protempa.PropertyConstraint;
import org.protempa.SimpleGapFunction;
import org.protempa.TemporalExtendedParameterDefinition;
import org.protempa.TemporalExtendedPropositionDefinition;
import org.protempa.proposition.value.AbsoluteTimeUnit;
import org.protempa.proposition.value.ValueType;

/**
 *
 * @author Andrew Post
 */
class ConversionUtil {

	static final String PRIMARY_PROP_ID_SUFFIX = "_PRIMARY";
	static final String VALUE = "YES";
	static final String VALUE_COMP = "NO";
	static final String PROP_ID_WRAPPED_SUFFIX = "_WRAPPED";
	static final String USER_KEY_PREFIX = "USER:";
	
	private static final PhenotypeConversionSupport CONVERSION_SUPPORT =
			new PhenotypeConversionSupport();
        private static Map<Long, String> timeUnitMap= new HashMap<>();
        private static Map<Long, String> relOperatorNameMap= new HashMap<>();
        private static Map<Long, String> frequencyTypeNameMap= new HashMap<>();
        private static Map<Long, String> thresholdOperationNameMap= new HashMap<>();
        private static Map<Long, String> valueComparatorNameMap= new HashMap<>();
        private static boolean isInitialized=false;
        @Inject
        static EurekaClinicalPhenotypeClient phenotypeClient;
        
        static {
            try {
                setupTimeUnitAndOperators();
            } catch (ClientException ex) {
                Logger.getLogger(ConversionUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        
        static void setTimeUnitMap(List<TimeUnit> listTimeUnit){
            timeUnitMap.clear();
            for(TimeUnit tu: listTimeUnit){
                timeUnitMap.put(tu.getId(), tu.getName());
            }
            
        }
        
        static void setRelOperatorNameMap(List<RelationOperator> listRelationOperator){
            relOperatorNameMap.clear();
            for(RelationOperator relOp: listRelationOperator){
                relOperatorNameMap.put(relOp.getId(), relOp.getName());
            }
            
        }
        
        static void setFrequencyTypeNameMap(List<FrequencyType> listFrequencyType){
            frequencyTypeNameMap.clear();
            for(FrequencyType relOp: listFrequencyType){
                frequencyTypeNameMap.put(relOp.getId(), relOp.getName());
            }
            
        }
        
        static void setThresholdOperationNameMap(List<ThresholdsOperator> listThresholdsOperator){
            listThresholdsOperator.clear();
            for(ThresholdsOperator item: listThresholdsOperator){
                thresholdOperationNameMap.put(item.getId(), item.getName());
            }
            
        }
        
        static void setValueComparatorNameMap(List<ValueComparator> listValueComparator){
            valueComparatorNameMap.clear();
            for(ValueComparator item: listValueComparator){
                valueComparatorNameMap.put(item.getId(), item.getName());
            }
            
        }

        public static void setupTimeUnitAndOperators() throws ClientException{
            if(!isInitialized){
                setupTimeUnitAndOperatorsForce();
                isInitialized = true;
            }
        }
                
        public static void setupTimeUnitAndOperatorsForce() throws ClientException{
            setTimeUnitMap(phenotypeClient.getTimeUnitsAsc());
            setRelOperatorNameMap(phenotypeClient.getRelationOperatorsAsc());
            setFrequencyTypeNameMap(phenotypeClient.getFrequencyTypesAsc());
            setThresholdOperationNameMap(phenotypeClient.getThresholdsOperators());
            setValueComparatorNameMap(phenotypeClient.getValueComparatorsAsc());
            
        }
        

//	static AbsoluteTimeUnit unit(TimeUnit unit) {
//		return unit != null ? AbsoluteTimeUnit.nameToUnit(unit.getName())
//				: null;
//	}
//        
        
        //TODO: replace this hard-coded function with RESTFUL to phenotype/protected/timeunits
        static AbsoluteTimeUnit unit(Long unit) {
                String unitName;
                if(unit == null){
                    unitName = "day";
                }
                else{
                 unitName = timeUnitMap.get(unit);
                 if (unitName ==null)
                     unitName = "day";
                }
		return unitName != null ? AbsoluteTimeUnit.nameToUnit(unitName)
				: null;
	}
        //
        static String relationOperatorName(Long relationOperator){
            if(relationOperator ==null)
                return null;
            String opName;
            opName = relOperatorNameMap.get(relationOperator);
            return opName;
        }
        //Frequency Type Name
        static String frequencyTypeName(Long frequencyType) {
                String frequencyTypeName;
                if(frequencyType == null)
                    return null;
                
                frequencyTypeName = frequencyTypeNameMap.get(frequencyType);
		return frequencyTypeName;
	}
        
        //ThresholdOp name
        static String thresholdOperationName(Long thresholdOp){
            if(thresholdOp == null)
                return null;
            String thresholdOpName;
            thresholdOpName = thresholdOperationNameMap.get(thresholdOp);
            
            return thresholdOpName;
        }
        
        //Comparator name
        
        static String valueComparatorName(Long compID){
            if(compID==null)
                return null;
         
            String compName;
            compName = valueComparatorNameMap.get(compID);
            return compName;
        }
        
        
        static String valueComparatorComplement(String comp){
            if(comp==null)
                return null;
            
            String compName;
            switch(comp){
                case "not=":
                    compName = "=";
                    break;
                case "=":
                    compName = "not=";
                    break;
                case "<=":
                    compName = ">";
                    break;
                case "<":
                    compName = ">=";
                    break;
                case ">=":
                    compName= "<";
                    break;
                case ">":
                    compName = "<=";
                    break;
                default:
                    compName = null;
            }
            return compName;
        }
    

	static TemporalExtendedPropositionDefinition buildExtendedPropositionDefinition(PhenotypeField phenotypeField) {
		TemporalExtendedPropositionDefinition tepd =
				buildExtendedPropositionDefinitionBarebone(phenotypeField);
		if (phenotypeField.getHasPropertyConstraint()!=null && phenotypeField.getHasPropertyConstraint()) {
			PropertyConstraint pc = new PropertyConstraint();
			pc.setPropertyName(
					phenotypeField.getProperty());
			pc.setValue(ValueType.VALUE.parse(phenotypeField.getPropertyValue()));
			pc.setValueComp(org.protempa.proposition.value.ValueComparator.EQUAL_TO);

			tepd.setPropertyConstraints(new PropertyConstraint[] {pc});
		}
		tepd.setMinLength(phenotypeField.getMinDuration());
		tepd.setMinLengthUnit(unit(phenotypeField.getMaxDurationUnits()));
		tepd.setMaxLength(phenotypeField.getMaxDuration());
		tepd.setMaxLengthUnit(unit(phenotypeField.getMinDurationUnits()));
		return tepd;
	}

	static ContextDefinition extractContextDefinition(
			ValueThresholds entity,
			List<PhenotypeField> extendedPhenotypes,
			ValueThreshold v) {
		ContextDefinition cd = new ContextDefinition(
				entity.getKey() + "_SUB_CONTEXT");
		cd.setGapFunction(new SimpleGapFunction(0, null));
		TemporalExtendedPropositionDefinition[] tepds =
				new TemporalExtendedPropositionDefinition[extendedPhenotypes.size()];
		int i = 0;
		for (PhenotypeField ede : extendedPhenotypes) {
			TemporalExtendedPropositionDefinition tepd;
			String tepdId = ede.getPhenotypeKey();
			if (!ede.isInSystem()) {
				tepdId = CONVERSION_SUPPORT.toPropositionId(tepdId);
			}
			if (ede.getType() == Phenotype.Type.VALUE_THRESHOLD) {
				TemporalExtendedParameterDefinition teParamD =
						new TemporalExtendedParameterDefinition(tepdId);
				//teParamD.setValue(CONVERSION_SUPPORT.asValue(ede));
                                teParamD.setValue(CONVERSION_SUPPORT.asValue(ede.getPhenotypeKey()));

				tepd = teParamD;
			} else {
				tepd = new TemporalExtendedPropositionDefinition(tepdId);
			}
			tepd.setDisplayName(ede.getPhenotypeDisplayName());
			tepd.setMaxLength(ede.getMaxDuration());
			tepd.setMaxLengthUnit(
					unit(ede.getMaxDurationUnits()));
			tepd.setMinLength(ede.getMinDuration());
			tepd.setMinLengthUnit(
					unit(ede.getMinDurationUnits()));
			tepds[i++] = tepd;
		}
		cd.setInducedBy(tepds);
		ContextOffset offset = new ContextOffset();
		Long relOp = v.getRelationOperator();
		Integer withinAtLeast = v.getWithinAtLeast();
		Integer withinAtMost = v.getWithinAtMost();
		String relOpName = relationOperatorName(relOp);
		if ("before".equals(relOpName)) {
			offset.setStartIntervalSide(Side.FINISH);
			offset.setFinishIntervalSide(Side.FINISH);
			if (withinAtLeast != null) {
				offset.setStartOffset(withinAtLeast);
			}
			offset.setStartOffsetUnits(unit(v.getWithinAtLeastUnit()));
			offset.setFinishOffset(withinAtMost);
			offset.setFinishOffsetUnits(unit(v.getWithinAtMostUnit()));
		} else if ("after".equals(relOpName)) {
			offset.setStartIntervalSide(Side.START);
			offset.setFinishIntervalSide(Side.START);
			offset.setStartOffset(withinAtMost != null ? -withinAtMost : null);
			offset.setStartOffsetUnits(unit(v.getWithinAtMostUnit()));
			if (withinAtLeast != null) {
				offset.setFinishOffset(-withinAtLeast);
			}
			offset.setFinishOffsetUnits(unit(v.getWithinAtLeastUnit()));
		} else if ("around".equals(relOpName)) {
			offset.setStartIntervalSide(Side.START);
			offset.setFinishIntervalSide(Side.FINISH);
			offset.setStartOffset(withinAtLeast != null ? -withinAtLeast : null);
			offset.setStartOffsetUnits(unit(v.getWithinAtLeastUnit()));
			offset.setFinishOffset(withinAtMost);
			offset.setFinishOffsetUnits(unit(v.getWithinAtMostUnit()));
		}
		cd.setOffset(offset);
		return cd;
	}

	private static TemporalExtendedPropositionDefinition buildExtendedPropositionDefinition(String propId,
			PhenotypeField phenotypeFiled) {
		TemporalExtendedPropositionDefinition tepd;
		if (phenotypeFiled.getType()==Phenotype.Type.VALUE_THRESHOLD) {
			TemporalExtendedParameterDefinition tepvDef =
					new TemporalExtendedParameterDefinition(propId);
			tepvDef.setValue(CONVERSION_SUPPORT.asValue(phenotypeFiled.getPhenotypeKey()));
			tepd = tepvDef;
		} else {
			tepd = new TemporalExtendedPropositionDefinition(propId);
		}
		return tepd;
	}

	private static TemporalExtendedPropositionDefinition buildExtendedPropositionDefinitionBarebone(
			PhenotypeField phenotypeFiled) {
		String propId;
		if (phenotypeFiled.isInSystem()) {
			propId = phenotypeFiled.getPhenotypeKey();
		} else {
			propId = CONVERSION_SUPPORT.toPropositionId(phenotypeFiled.getPhenotypeKey());
		}
		return buildExtendedPropositionDefinition(propId, phenotypeFiled);
	}
}
