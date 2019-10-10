/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package io.quarkus.it.hibernate.validator.post;

import javax.validation.constraints.NotNull;

public class JsonSerializableBean {
    @NotNull
    public String id;

    public JsonSerializableBean() {
    }

    public JsonSerializableBean(String id) {
        this.id = id;
    }
}
