package edu.emory.cci.aiw.cvrg.eureka.etl.entity;

/*-
 * #%L
 * Eureka! Clinical Protempa Service
 * %%
 * Copyright (C) 2012 - 2018 Emory University
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
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;


@Table(name = "usertemplates")
@Entity
public class UserTemplateEntity implements org.eurekaclinical.standardapis.entity.UserTemplateEntity<AuthorizedRoleEntity> {

    /**
     * The user's unique identifier.
     */
    @Id
    @SequenceGenerator(name = "UT_SEQ_GENERATOR", sequenceName = "UT_SEQ",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "UT_SEQ_GENERATOR")
    private Long id;

    /**
     * The user's email address.
     */
    @Column(unique = true, nullable = false)
    private String name;
    
    private boolean autoAuthorize;
    
    private String criteria;

    /**
     * A list of roles assigned to the user.
     */
    @ManyToMany(cascade = {CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "usertemplate_role",
            joinColumns = {
                @JoinColumn(name = "usertemplate_id")},
            inverseJoinColumns = {
                @JoinColumn(name = "role_id")})
    private List<AuthorizedRoleEntity> roles = new ArrayList<>();



    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long inId) {
        this.id = inId;
    }

    @Override
    public void setName(String inName) {
        this.name = inName;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public boolean isAutoAuthorize() {
        return autoAuthorize;
    }

    public void setAutoAuthorize(boolean autoAuthorize) {
        this.autoAuthorize = autoAuthorize;
    }

    /**
     * Gets the criteria for triggering auto-authorization. May be 
     * <code>null</code>, which means that auto-authorization will always be 
     * triggered when requested. The criteria are expressed as Freemarker 
     * Template Language expression
     * @return the criteria expression string.
     */
    public String getCriteria() {
        return criteria;
    }

    /**
     * Sets criteria for triggering auto-authorization. May be 
     * <code>null</code>, which means that auto-authorization will always be 
     * triggered when requested.
     * 
     * @param criteria the criteria for triggering auto-authorization, 
     * expressed as a Freemarker Template Language expression.
     */
    public void setCriteria(String criteria) {
        this.criteria = criteria;
    }

    @Override
    public List<AuthorizedRoleEntity> getRoles() {
        return new ArrayList<>(this.roles);
    }

    @Override
    public void setRoles(List<AuthorizedRoleEntity> inRoles) {
        if (inRoles == null) {
            this.roles = new ArrayList<>();
        } else {
            this.roles = new ArrayList<>(inRoles);
        }
    }
    
    @Override
    public void addRole(AuthorizedRoleEntity role) {
        if (!this.roles.contains(role)) {
            this.roles.add(role);
        }
    }
    
    @Override
    public void removeRole(AuthorizedRoleEntity role) {
        this.roles.remove(role);
    }


    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.id);
        return hash;
    }

    /**
     * Equal to another object of the same class if their id values are equal.
     * 
     * @param obj another object.
     * 
     * @return <code>true</code> if equal, <code>false</code> if not.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UserTemplateEntity other = (UserTemplateEntity) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UserTemplateEntity{" + "id=" + id + ", name=" + name + ", autoAuthorize=" + autoAuthorize + ", criteria=" + criteria + ", roles=" + roles +'}';
    }
}