package edu.emory.cci.aiw.cvrg.eureka.etl.dest;

import edu.emory.cci.aiw.cvrg.eureka.etl.entity.I2B2DestinationEntity;
/*
 * #%L
 * Eureka Protempa ETL
 * %%
 * Copyright (C) 2012 - 2015 Emory University
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
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.PhenotypeSearchDestinationEntity;

import edu.emory.cci.aiw.cvrg.eureka.etl.config.EtlProperties;
import edu.emory.cci.aiw.i2b2etl.dest.config.Concepts;
import edu.emory.cci.aiw.i2b2etl.dest.config.Configuration;
import edu.emory.cci.aiw.i2b2etl.dest.config.ConfigurationInitException;
import edu.emory.cci.aiw.i2b2etl.dest.config.Data;
import edu.emory.cci.aiw.etl.dest.config.Database;
import edu.emory.cci.aiw.i2b2etl.dest.config.Settings;


/**
 * Database-based phenotype search loader configuration.
 *
 * @author Nita Deshpande
 */
class EurekaPhenotypeSearchConfiguration implements Configuration {

    private final PhenotypeSearchDestinationEntity phenotypeSearchDestinationEntity;
    private final EtlProperties etlProperties;
    private PhenotypeSettings phenSettings;
    private PhenotypeSearchDatabase phenotypeSearchDatabase;
    private PhenotypeData phenData;
    private PhenotypeConcepts phenConcepts;



    EurekaPhenotypeSearchConfiguration(PhenotypeSearchDestinationEntity inPhenotypeSearchDestinationEntity, EtlProperties inEtlProperties) throws ConfigurationInitException {

        this.phenotypeSearchDestinationEntity = inPhenotypeSearchDestinationEntity;
        this.etlProperties = inEtlProperties;
        this.phenSettings = new PhenotypeSettings(this.phenotypeSearchDestinationEntity);
        this.phenotypeSearchDatabase = new PhenotypeSearchDatabase(this.phenotypeSearchDestinationEntity);
    }

	public PhenotypeSearchDestinationEntity getPhenotypeSearchDestinationEntity() {
		return phenotypeSearchDestinationEntity;
	}

	public EtlProperties getEtlProperties() {
		return etlProperties;
	}

	@Override
    public Database getDatabase() {
        return this.phenotypeSearchDatabase;
    }


    @Override
    public String getName() {
        return this.phenotypeSearchDestinationEntity.getName();
    }

	@Override
	public Concepts getConcepts() {
		if (this.phenConcepts == null) {
            this.phenConcepts = new PhenotypeConcepts(this.phenotypeSearchDestinationEntity.getConceptSpecs());
        }
        return this.phenConcepts;
    }

	@Override
	public Data getData() {
		if (this.phenData == null) {
            this.phenData = new PhenotypeData(this.phenotypeSearchDestinationEntity.getDataSpecs());
        }
        return this.phenData;
	}

	@Override
	public Settings getSettings() {
		return this.phenSettings;
	}

}
