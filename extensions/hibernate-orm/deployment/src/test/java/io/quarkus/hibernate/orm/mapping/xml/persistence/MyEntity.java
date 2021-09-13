package io.quarkus.hibernate.orm.mapping.xml.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class MyEntity {

    @Id
    public long id;

    public String name;

    public MyEntity() {
    }

    public MyEntity(String name) {
        this.name = name;
    }

}
