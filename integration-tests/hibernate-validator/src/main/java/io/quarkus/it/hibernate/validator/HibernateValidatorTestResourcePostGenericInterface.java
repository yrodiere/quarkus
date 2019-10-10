package io.quarkus.it.hibernate.validator;

import javax.validation.Valid;

public interface HibernateValidatorTestResourcePostGenericInterface<T> {

    String testRestPostEndPointGenericMethodValidation(@Valid T bean);

}
