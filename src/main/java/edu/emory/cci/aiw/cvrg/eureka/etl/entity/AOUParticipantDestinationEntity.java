package edu.emory.cci.aiw.cvrg.eureka.etl.entity;

/*
 * #%L
 * Eureka Common
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

/**
 *
 * @author Nita
 */
@Entity
@Table(name = "aou_participant_destinations")
public class AOUParticipantDestinationEntity extends DestinationEntity {
    
   
    private boolean alwaysQuoted = false;
    private String dataConnect;
    private String dataUser;
    private String dataPassword;

    public AOUParticipantDestinationEntity() {
        setDeidentificationEnabled(false);
    }

    public Character getDelimiter() {
        return ',';
    }
    
    public boolean isAlwaysQuoted() {
        return alwaysQuoted;
    }

    public void setAlwaysQuoted(boolean alwaysQuoted) {
        this.alwaysQuoted = alwaysQuoted;
    }
    
    public String getNullValue() {
        return null;
    }

    @Override
    public boolean isGetStatisticsSupported() {
        return false;
    }

    @Override
    public boolean isAllowingQueryPropositionIds() {
        return false;
    }

    @Override
    public void accept(DestinationEntityVisitor visitor) {
        visitor.visit(this);
    }

    public String getDataConnect() {
        return dataConnect;
    }

    public void setDataConnect(String dataConnect) {
        this.dataConnect = dataConnect;
    }

    public String getDataUser() {
        return dataUser;
    }

    public void setDataUser(String dataUser) {
        this.dataUser = dataUser;
    }

    public String getDataPassword() {
        return dataPassword;
    }

    public void setDataPassword(String dataPassword) {
        this.dataPassword = dataPassword;
    }
    
    
}
