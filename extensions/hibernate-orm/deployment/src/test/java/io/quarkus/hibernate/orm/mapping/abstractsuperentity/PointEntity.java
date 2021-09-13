package io.quarkus.hibernate.orm.mapping.abstractsuperentity;

import javax.persistence.Entity;

@Entity
public abstract class PointEntity extends DataIdentity {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}